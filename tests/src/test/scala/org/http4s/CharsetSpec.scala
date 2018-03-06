package org.http4s

import java.nio.charset.{Charset => NioCharset}

class CharsetSpec extends Http4sSpec {
  "fromString" should {
    "be case insensitive" in {
      prop { cs: NioCharset =>
        val upper = cs.name.toUpperCase
        val lower = cs.name.toLowerCase
        Charset.fromString(upper) must_== Charset.fromString(lower)
      }
    }

    "work for aliases" in {
      Charset.fromString("UTF8") must beRight(Charset.`UTF-8`)
    }

    "return InvalidCharset for unregistered names" in {
      Charset.fromString("blah") must beLeft
    }
  }
}
