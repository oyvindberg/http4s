package org.http4s.circe

import java.nio.ByteBuffer

import cats.effect.Sync
import io.circe.{Json, Printer}
import org.http4s.{
  DecodeResult,
  EntityDecoder,
  MalformedMessageBodyFailure,
  MediaType,
  Message,
  jawn
}

trait CirceInstances extends CirceInstancesBase {
  import io.circe.jawn.CirceSupportParser.facade
  import io.circe.jawn._

  def jsonDecoderIncremental[F[_]: Sync]: EntityDecoder[F, Json] =
    jawn.jawnDecoder[F, Json]

  def jsonDecoderByteBuffer[F[_]: Sync]: EntityDecoder[F, Json] =
    EntityDecoder.decodeBy(MediaType.application.json)(jsonDecoderByteBufferImpl[F])

  private def jsonDecoderByteBufferImpl[F[_]: Sync](msg: Message[F]): DecodeResult[F, Json] =
    EntityDecoder.collectBinary(msg).flatMap { segment =>
      val bb = ByteBuffer.wrap(segment.force.toArray)
      if (bb.hasRemaining) {
        parseByteBuffer(bb) match {
          case Right(json) =>
            DecodeResult.success[F, Json](json)
          case Left(pf) =>
            DecodeResult.failure[F, Json](
              MalformedMessageBodyFailure("Invalid JSON", Some(pf.underlying)))
        }
      } else {
        DecodeResult.failure[F, Json](MalformedMessageBodyFailure("Invalid JSON: empty body", None))
      }
    }

  def jsonDecoderAdaptive[F[_]: Sync](cutoff: Long): EntityDecoder[F, Json] =
    EntityDecoder.decodeBy(MediaType.application.json) { msg =>
      msg.contentLength match {
        case Some(contentLength) if contentLength < cutoff =>
          jsonDecoderByteBufferImpl[F](msg)
        case _ => jawn.jawnDecoderImpl[F, Json](msg)
      }
    }
}

object CirceInstances {
  def withPrinter(p: Printer): CirceInstances =
    new CirceInstances {
      val defaultPrinter: Printer = p
      def jsonDecoder[F[_]: Sync]: EntityDecoder[F, Json] = defaultJsonDecoder
    }

  // default cutoff value is based on benchmarks results
  def defaultJsonDecoder[F[_]: Sync]: EntityDecoder[F, Json] =
    jsonDecoderAdaptive(cutoff = 100000)
}
