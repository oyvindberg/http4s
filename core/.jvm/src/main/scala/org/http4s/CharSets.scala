package org.http4s

import java.nio.charset.{Charset => NioCharset}
import scala.collection.JavaConverters._

object CharSets {
  val availableCharsets: Seq[NioCharset] =
    NioCharset.availableCharsets.values.asScala.toSeq
}
