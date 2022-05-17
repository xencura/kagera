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

import io.kagera.api.colored.dsl._

import scala.concurrent.ExecutionContext

object Generators {

  object Uncolored {
    def sequence(nrOfSteps: Int, automated: Boolean = false): ExecutablePetriNet[Unit] = {

      val transitions = (1 to nrOfSteps).map(i => nullTransition(id = i, automated = automated))
      val places = (1 to (nrOfSteps - 1)).map(i => Place[Unit](id = i))
      val tpedges = transitions.zip(places).map { case (t, p) => arc(t, p, 1) }
      val ptedges = places.zip(transitions.tail).map { case (p, t) => arc(p, t, 1) }

      process[Unit]((tpedges ++ ptedges): _*)
    }
  }
}
