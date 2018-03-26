package org.http4s.client

import scala.scalajs.js

package object fetchclient {

  /**
    * Since fetch support is not ubiquitous, this supports
    *  that user can plug in a polyfill
    */
  type Fetch =
    js.Function2[Fetch.RequestInfo, js.UndefOr[Fetch.RequestInit], js.Promise[Fetch.Response]]

  object Fetch {
    import org.scalajs.dom.experimental

    type BodyInit = experimental.BodyInit
    type Chunk[+T] = experimental.Chunk[T]
    type Headers = experimental.Headers
    type HeadersInit = experimental.HeadersInit
    type HttpMethod = experimental.HttpMethod
    type ReadableStreamReader[+T] = experimental.ReadableStreamReader[T]
    type ReferrerPolicy = experimental.ReferrerPolicy
    type RequestCache = experimental.RequestCache
    type RequestCredentials = experimental.RequestCredentials
    type RequestInfo = experimental.RequestInfo
    type RequestInit = experimental.RequestInit
    type RequestMode = experimental.RequestMode
    type RequestRedirect = experimental.RequestRedirect
    type Response = experimental.Response

    val RequestInit = experimental.RequestInit
    val ReferrerPolicy = experimental.ReferrerPolicy
    val RequestMode = experimental.RequestMode
    val RequestCredentials = experimental.RequestCredentials
    val RequestCache = experimental.RequestCache
    val RequestRedirect = experimental.RequestRedirect
  }
}
