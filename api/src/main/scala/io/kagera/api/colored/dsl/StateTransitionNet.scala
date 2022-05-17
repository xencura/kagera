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

package io.kagera.api.colored.dsl

import cats.effect.Sync
import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransition }
import io.kagera.api.colored.{ Marking, Transition, _ }

import scala.concurrent.duration.Duration
import scala.util.Random

trait StateTransitionNet[S, E] {

  def eventSourcing: S => E => S

  def transition(
    id: Long = Math.abs(Random.nextLong()),
    label: Option[String] = None,
    automated: Boolean = false,
    exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockTransition
  )(fn: S => E): Transition[Unit, E, S] =
    new AbstractTransition[Unit, E, S](id, label.getOrElse(s"t$id"), automated, Duration.Undefined, exceptionStrategy)
      with UncoloredTransition[Unit, E, S] {
      override val toString = label
      override val updateState = eventSourcing
      override def produceEvent[F[_]](consume: Marking, state: S, input: Unit)(implicit sync: Sync[F]): F[E] =
        Sync.apply.delay { (fn(state)) }
    }

  def createPetriNet(arcs: Arc*) = process[S](arcs: _*)
}
