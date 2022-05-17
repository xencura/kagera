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

package io.kagera.execution

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api.colored.{ ColoredPetriNet, Marking, Transition, TransitionFunction }

trait TransitionExecutor[F[_], State] {

  /**
   * Given a transition returns an input output function
   *
   * @param t
   * @tparam Input
   * @tparam Output
   * @return
   */
  def fireTransition[Input, Output](t: Transition[Input, Output, State]): TransitionFunction[F, Input, Output, State]
}

class TransitionExecutorImpl[F[_], State](topology: ColoredPetriNet)(implicit
  sync: Sync[F],
  errorHandling: ApplicativeError[F, Throwable]
) extends TransitionExecutor[F, State] {

  val cachedTransitionFunctions: Map[Transition[_, _, _], _] =
    topology.transitions.map(t => t -> t.apply[F](topology.inMarking(t), topology.outMarking(t))).toMap

  def transitionFunction[Input, Output](t: Transition[Input, Output, State]) =
    cachedTransitionFunctions(t).asInstanceOf[TransitionFunction[F, Input, Output, State]]

  def fireTransition[Input, Output](
    t: Transition[Input, Output, State]
  ): TransitionFunction[F, Input, Output, State] = { (consume, state, input) =>
    def handleFailure: PartialFunction[Throwable, F[(Marking, Output)]] = { case e: Throwable =>
      errorHandling.raiseError(e).asInstanceOf[F[(Marking, Output)]]
    }

    if (consume.multiplicities != topology.inMarking(t)) {
      errorHandling.raiseError(new IllegalArgumentException(s"Transition $t may not consume $consume"))
    }

    try {
      errorHandling.handleErrorWith(transitionFunction[Input, Output](t)(consume, state, input)) { handleFailure }
    } catch { handleFailure }
  }
}
