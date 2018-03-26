package org.http4s
package client
package fetchclient

import cats.data.Kleisli
import cats.effect._
import cats.implicits.{catsSyntaxEither => _, _}
import fs2.Stream._
import fs2._
import org.scalajs.dom.crypto.BufferSource

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.{Int8Array, Uint8Array}
import scala.scalajs.js.|

object FetchClient {

  /**
    * Create an HTTP client based on the AsyncHttpClient library
    *
    * @param config configuration for the client
    * @param bufferSize body chunks to buffer when reading the body; defaults to 8
    * @param ec The ExecutionContext to run responses on
    */
  def apply[F[_]](config: FetchConfig)(implicit F: Effect[F], ec: ExecutionContext): Client[F] =
    Client(
      Kleisli { req =>
        F.flatMap(toRequest(req, config))(fetchReq =>
          F.async[DisposableResponse[F]] { cb =>
            println("Requesting " + req.uri.renderString)
            val ret = config
              .fetch(req.uri.renderString, fetchReq)
              .`then`[Unit](
                res => handleSuccess(res, cb),
                js.defined(rejected => cb(Left(handleJsError(rejected))))
              )
            println("Requested " + ret)
            ()

        })
      },
      F.pure(())
    )

  def handleJsError(error: Any): Throwable =
    error match {
      case th: Throwable =>
        th
      case error: js.Error =>
        //todo: upstream this missing field
        val stack = error.asInstanceOf[js.Dynamic].stack.asInstanceOf[js.UndefOr[String]]
        val msg = stack.getOrElse(s"${error.name}: ${error.message}")
        InvalidResponseException(s"Got javascript error. $msg")
      case other =>
        InvalidResponseException(other.toString)
    }

  private def toRequest[F[_]: Effect](in: Request[F], config: FetchConfig): F[Fetch.RequestInit] = {
    def method(m: Method): Fetch.HttpMethod =
      m.name.asInstanceOf[Fetch.HttpMethod]

    def headers(hs: Headers): Fetch.HeadersInit =
      js.Dictionary(hs.map(h => h.name.value -> h.value).toSeq: _*)

    //todo: investigate further if it's possible to stream the request body
    def body(e: EntityBody[F]): F[js.UndefOr[Fetch.BodyInit]] =
      e match {
        case EmptyBody => Effect[F].pure(js.undefined)
        case e => e.compile.toVector.map(v => new Int8Array(v.toJSArray): BufferSource)
      }

    body(in.body).map(
      b =>
        Fetch.RequestInit(
          method = method(in.method),
          headers = headers(in.headers),
          body = b,
          referrer = config.referrer,
          referrerPolicy = config.referrerPolicy,
          mode = config.mode,
          credentials = config.credentials,
          requestCache = config.requestCache,
          requestRedirect = config.requestRedirect,
          integrity = config.integrity,
          window = config.window
      )
    )
  }

  def responseBodyStream[F[_]: Effect](reader: Fetch.ReadableStreamReader[Uint8Array])(
      implicit ec: ExecutionContext): EntityBody[F] = {
    val stream = for {
      q <- Stream.eval(async.unboundedQueue[F, Either[Throwable, Fetch.Chunk[Uint8Array]]])
      _ <- Stream.suspend {
        val yes: js.Function1[Fetch.Chunk[Uint8Array], Unit | js.Thenable[Unit]] =
          yes => async.unsafeRunAsync(q.enqueue1(Right(yes)))(_ => IO.unit)

        reader.read().`then`(yes)
        Stream.emit(())
      }
      row <- q.dequeue.rethrow
    } yield row

    stream.flatMap((chunk: Fetch.Chunk[Uint8Array]) =>
      Stream.chunk(new Bytes(chunk.value, 0, chunk.value.length)))
  }

  def fromFetchHeaders(hs: Fetch.Headers): Headers = {
    /* prefer `js.Dictionary` interface instead of using `hs.jsIterator()` directly
        because that assumes `Symbol` is present, which is not the case for node.js */
    println(hs)
    println(js.JSON.stringify(hs))
    val hs2 = hs.asInstanceOf[js.Dictionary[js.Array[String]]]
    try {
      val ret = Headers(hs2.map { case (k, v) => Header(k, v(0)) }.toList)
      println(ret)
      ret
    } catch {
      case x: Throwable =>
        println(x)
        x.printStackTrace()
        Headers()
    }
  }

  def handleSuccess[F[_]: Effect](
      res: Fetch.Response,
      cb: (Either[Throwable, DisposableResponse[F]] => Unit))(
      implicit ec: ExecutionContext): Unit = {

    println("fooooo" + res.status)
    val reader: Fetch.ReadableStreamReader[Uint8Array] =
      res.body.getReader()

    val dispose: F[Unit] =
      Effect[F].async { cb =>
        val no: js.UndefOr[js.Function1[Any, Unit | js.Thenable[Unit]]] =
          js.defined(err => cb(Left(handleJsError(err))))

        val yes: js.Function1[Any, Unit | js.Thenable[Unit]] =
          _ => cb(Right(()))

        reader.cancel("Disposed").`then`[Unit](yes, no)
        ()
      }

    val body: EntityBody[F] =
      responseBodyStream[F](reader)
    val dr = DisposableResponse(
      Response(
        status = Status.fromInt(res.status).valueOr(throw _),
        headers = fromFetchHeaders(res.headers),
        body = body
      ),
      dispose
    )

    cb(Right(dr))
  }

  final case class Bytes(values: Uint8Array, offset: Int, length: Int) extends Chunk[Byte] {
    Bytes.checkBounds(values, offset, length)
    def size: Int = length
    def apply(i: Int): Byte = values(offset + i).asInstanceOf[Byte]
    def at(i: Int) = values(offset + i)
    protected def splitAtChunk_(n: Int): (Chunk[Byte], Chunk[Byte]) =
      Bytes(values, offset, n) -> Bytes(values, offset + n, length - n)
    override def toArray[O2 >: Byte: ClassTag]: Array[O2] =
      values.slice(offset, offset + length).toArray.asInstanceOf[Array[O2]]
  }
  object Bytes {
    def checkBounds(values: Uint8Array, offset: Int, length: Int): Unit = {
      require(offset >= 0 && offset <= values.size)
      require(length >= 0 && length <= values.size)
      val end = offset + length
      require(end >= 0 && end <= values.size)
    }
  }
}
