package org.http4s.client.fetchclient

import scala.scalajs.js

trait FetchConfig {
  val fetch: Fetch
  var referrer: js.UndefOr[String] = js.undefined
  var referrerPolicy: js.UndefOr[Fetch.ReferrerPolicy] = js.undefined
  var mode: js.UndefOr[Fetch.RequestMode] = js.undefined
  var credentials: js.UndefOr[Fetch.RequestCredentials] = js.undefined
  var requestCache: js.UndefOr[Fetch.RequestCache] = js.undefined
  var requestRedirect: js.UndefOr[Fetch.RequestRedirect] = js.undefined
  //see [[https://w3c.github.io/weba-subresource-integrity/ integrity spec]]
  var integrity: js.UndefOr[String] = js.undefined
  var window: js.UndefOr[Null] = js.undefined
}
