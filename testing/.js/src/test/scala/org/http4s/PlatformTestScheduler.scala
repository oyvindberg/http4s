/* checkAll and friends were copied from the scalaz-specs2 project.
 * Source file: src/main/scala/Spec.scala
 * Project address: https://github.com/typelevel/scalaz-specs2
 * Copyright (C) 2013 Lars Hupel
 * License: MIT. https://github.com/typelevel/scalaz-specs2/blob/master/LICENSE.txt
 * Commit df921e18cf8bf0fd0bb510133f1ca6e1caea512b
 * Copied on. 11/1/2015
 */

package org.http4s

import fs2._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js.timers._

object PlatformTestScheduler {

  //stolen from fs2/core/js/src/main/scala/fs2/SchedulerPlatform.scala
  val TestScheduler: Scheduler = new Scheduler {
    override def scheduleOnce(delay: FiniteDuration)(thunk: => Unit): () => Unit = {
      val handle = setTimeout(delay)(thunk)
      () =>
        { clearTimeout(handle) }
    }
    override def scheduleAtFixedRate(period: FiniteDuration)(thunk: => Unit): () => Unit = {
      val handle = setInterval(period)(thunk)
      () =>
        { clearInterval(handle) }
    }
    override def toString = "Scheduler"
  }

  val TestExecutionContext: ExecutionContext = JSExecutionContext.queue
}
