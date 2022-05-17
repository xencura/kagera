/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.api.colored

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class ColoredMarkingSpec extends AnyWordSpec {

  case class Person(name: String, age: Int)

  val p1 = Place[Int](id = 1)
  val p2 = Place[String](id = 2)
  val p3 = Place[Double](id = 3)
  val p4 = Place[Person](id = 4)

  "A Colored Marking" should {

    "correctly implement the multiplicity function" in {

      val m = Marking(p1(1, 2), p2("foo", "bar"))

      m.multiplicities shouldBe Map(p1 -> 2, p2 -> 2)
    }

    "have correct produce semantics" in {

      val m1 = Marking(p1(1, 2), p2("foo", "bar"))

      m1 |+| Marking.empty shouldBe m1

      val m2 = Marking(p1(3), p2("baz"), p3(1d))

      m1 |+| m2 shouldBe Marking(p1(3, 1, 2), p2("baz", "foo", "bar"), p3(1d))
    }

    "have correct consume semantics" in {

      val m1: Marking = Marking(p1(1, 2, 3), p2("foo", "bar"), p4(Person("Joe", 42)))

      m1 |-| Marking.empty shouldBe m1

      val m2 = Marking(p1(2), p4(Person("Joe", 42)))

      m1 |-| m2 shouldBe Marking(p1(1, 3), p2("foo", "bar"))
    }

    "in case of token value equality only consume tokens equal to the multiplicity" in {

      val m1 = Marking(p1(1, 1, 1, 1, 1))
      val m2 = Marking(p1(1, 1))

      m1 |-| m2 shouldBe Marking(p1(1, 1, 1))
    }
  }
}
