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
import io.kagera.api.colored.dsl._
import org.scalatest.wordspec.AnyWordSpec

class TokenGameSpec extends AnyWordSpec {

  val p1 = Place[Int](id = 1)
  val p2 = Place[Int](id = 2)

  val t1 = constantTransition[Int, Int, Unit](id = 1, automated = false, constant = 42)
  val t2 = constantTransition[Int, Int, Unit](id = 2, automated = false, constant = 5)

  val testProcess = process(p1 ~> (t1, filter = _ > 42), p1 ~> (t2, weight = 3), t1 ~> p2, t2 ~> p2)

  "The Colored Token game" should {

    "NOT mark a transition enabled if there are NOT enough tokens in in-adjacent places" in {

      val marking = Marking(p1(10, 10))

      // the multiplicity of p1 is 2
      marking.multiplicities(p1) should be(2)

      // t2 requires at least 3 tokens in p1 and is therefor not enabled
      testProcess.isEnabled(marking)(t2) should be(false)
    }

    "DO mark a transition as enabled if there ARE enough tokens in in-adjacent places" in {
      val marking = Marking(p1(10, 10, 10))

      // the multiplicity of p1 is 2
      marking.multiplicities(p1) should be(3)

      // t2 requires at least 3 tokens in p1 and is therefor not enabled
      testProcess.isEnabled(marking)(t2) should be(true)
    }

    "NOT mark a transition enabled if an in-adjacent edge filters the token" in {

      val marking = Marking(p1(10))

      testProcess.isEnabled(marking)(t1) should be(false)
    }
  }
}
