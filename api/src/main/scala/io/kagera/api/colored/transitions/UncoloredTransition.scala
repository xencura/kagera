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

package io.kagera.api.colored.transitions

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api.colored._
import io.kagera.api.multiset.MultiSet

trait UncoloredTransition[Input, Output, State] extends Transition[Input, Output, State] {

  override def apply[F[_]](inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
    sync: Sync[F],
    errorHandling: ApplicativeError[F, Throwable]
  ) = { (consume, state, input) =>
    {
      // assumes uncolored outgoing places (Place[Unit])
      val produce = outAdjacent.map { case (p, count) => p -> Map(() -> count) }.toMarking
      sync.map(produceEvent[F](consume, state, input))(output => (produce, output))
    }
  }

  def produceEvent[F[_] : Sync](consume: Marking, state: State, input: Input): F[Output]
}
