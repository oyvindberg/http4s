//package org.http4s.client.fetchclient
//
//import cats.data.Kleisli
//import cats.effect._
//import cats.effect.implicits._
//import cats.implicits.{catsSyntaxEither => _, _}
//import fs2.Stream._
//import fs2._
//import fs2.async.mutable.Queue
//import fs2.interop.reactivestreams.{StreamSubscriber, StreamUnicastPublisher}
//import org.http4s.{
//  EntityBody,
//  Header,
//  Headers,
//  InvalidResponseException,
//  Method,
//  Request,
//  Response,
//  Status
//}
//import org.http4s.client.{Client, DisposableResponse}
//import org.reactivestreams.{Publisher, Subscriber}
//import org.scalajs.dom.crypto.BufferSource
//import org.scalajs.dom.experimental.ReadableStreamReader
//
//import scala.concurrent.ExecutionContext
//import org.scalajs.dom.{experimental, experimental => Fetch}
//
//import scala.scalajs.js
//import scala.scalajs.js.JSConverters._
//import scala.scalajs.js.Promise
//import scala.scalajs.js.typedarray.{Int8Array, Uint8Array}
//
//object FetchClient {
//  case class FetchConfig()
//  val defaultConfig = FetchConfig()
//
//  //  val defaultConfig = new DefaultAsyncHttpClientConfig.Builder()
////    .setMaxConnectionsPerHost(200)
////    .setMaxConnections(400)
////    .setRequestTimeout(30000)
////    .setThreadFactory(threadFactory(name = { i =>
////      s"http4s-async-http-client-worker-${i}"
////    }))
////    .build()
//
//  /**
//    * Create an HTTP client based on the AsyncHttpClient library
//    *
//    * @param config configuration for the client
//    * @param bufferSize body chunks to buffer when reading the body; defaults to 8
//    * @param ec The ExecutionContext to run responses on
//    */
//  def apply[F[_]](
//      config: FetchConfig = defaultConfig)(implicit F: Effect[F], ec: ExecutionContext): Client[F] =
//    Client(
//      Kleisli { req =>
//        F.flatMap(toRequest(req))(fetchReq =>
//          F.async[DisposableResponse[F]] { cb =>
//            Fetch.Fetch
//              .fetch(req.uri.renderString, fetchReq)
//              .`then`[Unit](
//                res => handleSuccess(res, cb),
//                js.defined(rejected => cb(Left(handleJsError(rejected))))
//              )
//            ()
//        })
//      },
//      F.pure(())
//    )
//
//  def handleJsError(error: Any): Throwable =
//    error match {
//      case th: Throwable =>
//        th
//      case error: js.Error =>
//        //todo: upstream this missing field
//        val stack = error.asInstanceOf[js.Dynamic].stack.asInstanceOf[js.UndefOr[String]]
//        InvalidResponseException(
//          s"Got javascript error ${error.name}: ${error.message}.${stack.fold("")(" Stacktrace: " + _)}"
//        )
//      case other =>
//        InvalidResponseException(other.toString)
//    }
//
//  private def toRequest[F[_]: Effect](in: Request[F])(
//      implicit ec: ExecutionContext): F[Fetch.RequestInit] = {
//    def method(m: Method): Fetch.HttpMethod =
//      m.name.asInstanceOf[Fetch.HttpMethod]
//
//    def headers(hs: Headers): Fetch.Headers = {
//      val ret = new Fetch.Headers()
//      hs.foreach(h => ret.append(h.name.value, h.value))
//      ret
//    }
//
//    //todo: investigate further if it's possible to stream the request body
//    def body(e: EntityBody[F]): F[Fetch.BodyInit] =
//      e.compile.toVector.map(v => new Int8Array(v.toJSArray): BufferSource)
//
//    body(in.body).map(
//      b =>
//        Fetch.RequestInit(
//          method = method(in.method),
//          headers = headers(in.headers),
//          body = b
//      )
//    )
//
//  }
//
//  def asdasd[F[_]: Effect](
//      res: Fetch.Response,
//      cb: (Either[Throwable, DisposableResponse[F]] => Unit))(
//      implicit ec: ExecutionContext): Stream[F, Byte] = {
//    val reader: ReadableStreamReader[Uint8Array] =
//      res.body.getReader()
//
//    val stream = for {
//      q <- Stream.eval(async.unboundedQueue[F, Either[Throwable, Fetch.Chunk[Uint8Array]]])
//
//      _ <- Stream.suspend {
//        reader
//          .read()
//          .`then`(
//            yes => async.unsafeRunAsync(q.enqueue1(Right(yes)))(_ => IO.unit)
//          )
//        Stream.emit(())
//      }
//      row <- q.dequeue.rethrow
//    } yield row
//
//    stream.flatMap(chunk => Stream.chunk(Chunk.bytes(chunk.value.toArray[Byte])))
//  }
//
//  def handleSuccess[F[_]: Effect](
//      res: Fetch.Response,
//      cb: (Either[Throwable, DisposableResponse[F]] => Unit)): Unit = {
//    def headers(hs: Fetch.Headers): Headers =
//      Headers(res.headers.jsIterator().toIterator.toList.map { header =>
//        Header(header(0), header(1))
//      })
//
//    val reader = res.body.getReader()
//
//    val dispose: F[Unit] =
//      Effect[F].async(cb => reader.cancel("Cancelled").`then`(_ => cb(Right(()))))
//
//    val body: EntityBody[F] = ???
//
//    var dr = DisposableResponse(
//      Response(
//        status = Status.fromInt(res.status).valueOr(throw _),
//        headers = headers(res.headers),
//        body = body
//      ),
//      dispose
//    )
//
//    def go = {
//      val das: Promise[Fetch.Chunk[Uint8Array]] = reader.read()
//    }
//    dr
//
//    StreamSubscriber[F, Fetch.Chunk[Uint8Array]]()
//      .map { subscriber =>
//        val body = subscriber.stream.flatMap((part: Fetch.Chunk[Uint8Array]) =>
//          chunk(Chunk.bytes(part.toJSArray)))
//        dr = dr.copy(
//          response = dr.response.copy(body = body),
//          dispose = F.delay(state = State.ABORT)
//        )
//        // Run this before we return the response, lest we violate
//        // Rule 3.16 of the reactive streams spec.
//        publisher.subscribe(subscriber)
//        // We have a fully formed response now.  Complete the
//        // callback, rather than waiting for onComplete, or else we'll
//        // buffer the entire response before we return it for
//        // streaming consumption.
//        ec.execute(new Runnable { def run(): Unit = cb(Right(dr)) })
//      }
//      .runAsync(_ => IO.unit)
//      .unsafeRunSync
//
//  }
//
//  private def asyncHandler[F[_]](
//      cb: Callback[DisposableResponse[F]])(implicit F: Effect[F], ec: ExecutionContext) =
//    new StreamedAsyncHandler[Unit] {
//      var state: State = State.CONTINUE
//      var dr: DisposableResponse[F] =
//        DisposableResponse[F](Response(), F.delay(state = State.ABORT))
//
//      override def onStream(publisher: Publisher[HttpResponseBodyPart]): State = {
//        // backpressure is handled by requests to the reactive streams subscription
//        StreamSubscriber[F, HttpResponseBodyPart]()
//          .map { subscriber =>
//            val body = subscriber.stream.flatMap(part => chunk(Chunk.bytes(part.getBodyPartBytes)))
//            dr = dr.copy(
//              response = dr.response.copy(body = body),
//              dispose = F.delay(state = State.ABORT)
//            )
//            // Run this before we return the response, lest we violate
//            // Rule 3.16 of the reactive streams spec.
//            publisher.subscribe(subscriber)
//            // We have a fully formed response now.  Complete the
//            // callback, rather than waiting for onComplete, or else we'll
//            // buffer the entire response before we return it for
//            // streaming consumption.
//            ec.execute(new Runnable { def run(): Unit = cb(Right(dr)) })
//          }
//          .runAsync(_ => IO.unit)
//          .unsafeRunSync
//        state
//      }
//
//      override def onBodyPartReceived(httpResponseBodyPart: HttpResponseBodyPart): State =
//        throw org.http4s.util.bug("Expected it to call onStream instead.")
//
//      override def onStatusReceived(status: HttpResponseStatus): State = {
//        dr = dr.copy(response = dr.response.copy(status = getStatus(status)))
//        state
//      }
//
//      override def onHeadersReceived(headers: HttpResponseHeaders): State = {
//        dr = dr.copy(response = dr.response.copy(headers = getHeaders(headers)))
//        state
//      }
//
//      override def onThrowable(throwable: Throwable): Unit =
//        ec.execute(new Runnable { def run(): Unit = cb(Left(throwable)) })
//
//      override def onCompleted(): Unit = {
//        // Don't close here.  onStream may still be being called.
//      }
//    }
//}
