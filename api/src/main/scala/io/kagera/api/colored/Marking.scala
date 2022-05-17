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

import io.kagera.api.HMap
import io.kagera.api.multiset._

object Marking {

  type MarkingData = Map[Place[_], MultiSet[_]]

  def empty: Marking = HMap[Place, MultiSet](Map.empty)

  def apply[A](m1: MarkedPlace[A]): Marking = {
    HMap[Place, MultiSet](Map(m1): MarkingData)
  }

  def apply[A, B](m1: MarkedPlace[A], m2: MarkedPlace[B]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2): MarkingData)
  }

  def apply[A, B, C](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3): MarkingData)
  }

  def apply[A, B, C, D](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C], m4: MarkedPlace[D]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3, m4): MarkingData)
  }

  def apply[A, B, C, D, E](
    m1: MarkedPlace[A],
    m2: MarkedPlace[B],
    m3: MarkedPlace[C],
    m4: MarkedPlace[D],
    m5: MarkedPlace[E]
  ): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3, m4, m5): MarkingData)
  }
}
