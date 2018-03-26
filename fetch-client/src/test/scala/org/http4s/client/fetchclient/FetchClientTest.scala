package org.http4s.client.fetchclient

import cats.effect.{Effect, IO}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Http4sSpec, Request, Status, Uri}

class FetchClientTest extends Http4sSpec with Http4sClientDsl[IO] {

  val config = new FetchConfig {
    override val fetch: Fetch = FetchModule.requireFrom("node-fetch-polyfill")
    requestCache = Fetch.RequestCache.`no-cache`
  }

  val client = FetchClient(config)(Effect[IO], Http4sSpec.TestExecutionContext)

  "FetchClient" should {
    "Strip fragments from URI" in {
      val uri = Uri.uri("http://se.wikipedia.org/wiki/Buckethead_discography#Studio_albums")
      val body: IO[Status] = client.fetch(Request[IO](uri = uri))(e => {
        println(e.status)
        IO.pure(e.status)
      })

      body.unsafeRunAsync(e => println(e))

      success("mas")
    }

    "fetch body" in {
      val uri = Uri.uri("https://en.wikipedia.org/wiki/Buckethead_discography#Studio_albums")
      val body = client.fetch(Request[IO](uri = uri))(e => IO.pure(e.bodyAsText))
      body.unsafeRunAsync(e => println(e))
      success("mas")
    }
  }
}
