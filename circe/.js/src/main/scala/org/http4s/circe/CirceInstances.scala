package org.http4s.circe

import cats.effect.Sync
import io.circe.scalajs.convertJsToJson
import io.circe.{Json, Printer}
import org.http4s.{Charset, DecodeResult, EntityDecoder, MalformedMessageBodyFailure, MediaType}

import scala.scalajs.js
import scala.scalajs.js.JSON

trait CirceInstances extends CirceInstancesBase {
  def jsonDecoder2[F[_]: Sync]: EntityDecoder[F, Json] =
    EntityDecoder.decodeBy(MediaType.application.json) { msg =>
      EntityDecoder.collectBinary(msg).flatMap { segment =>
        val charSet = msg.charset.getOrElse(Charset.`UTF-8`).nioCharset
        val s = new String(segment.force.toArray, charSet)
        convertJsToJson(JSON.parse(s).asInstanceOf[js.Any]) match {
          case Right(json) =>
            DecodeResult.success[F, Json](json)
          case Left(pf) =>
            DecodeResult.failure[F, Json](MalformedMessageBodyFailure("Invalid JSON", Some(pf)))
        }
      }
    }
  def jsonDecoder[F[_]: Sync]: EntityDecoder[F, Json] = jsonDecoder2

}

object CirceInstances {
  def withPrinter(p: Printer): CirceInstances =
    new CirceInstances {
      val defaultPrinter: Printer = p
    }

  // default cutoff value is based on benchmarks results
  def defaultJsonDecoder[F[_]: Sync]: EntityDecoder[F, Json] =
    jsonDecoder2

}
