package org.http4s.client.fetchclient

import scala.scalajs.js

object FetchModule {

  val globalFetch: Fetch = js.Dynamic.global.fetch.asInstanceOf[Fetch]

  def requireFrom(name: String): Fetch =
    js.Dynamic.global.require(name).asInstanceOf[Fetch] match {
      case no if js.isUndefined(no) || js.typeOf(no) != "function" =>
        throw new Error(s"Javascript module $name did not contain a fetch function")
      case yes => yes
    }
}
