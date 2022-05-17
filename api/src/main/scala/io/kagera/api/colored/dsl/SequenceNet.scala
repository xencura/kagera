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
import io.kagera.api.colored._
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransition }

import scala.concurrent.duration.Duration

case class TransitionBehaviour[S, E](automated: Boolean, exceptionHandler: TransitionExceptionHandler, fn: S => E) {
  def asTransition(id: Long, eventSource: S => E => S) =
    new AbstractTransition[Unit, E, S](id, s"t$id", automated, Duration.Undefined, exceptionHandler)
      with UncoloredTransition[Unit, E, S] {
      override val toString = label
      override val updateState = eventSource
      override def produceEvent[F[_] : Sync](consume: Marking, state: S, input: Unit): F[E] = Sync.apply.delay {
        (fn(state))
      }
    }
}

trait SequenceNet[S, E] {

  def sequence: Seq[TransitionBehaviour[S, E]]
  def eventSourcing: S => E => S

  lazy val places = (1 to (sequence.size + 1)).map(i => Place[Unit](id = i))
  lazy val transitions = petriNet.transitions
  lazy val initialMarking = Marking(place(1) -> 1)

  def place(n: Int) = places(n - 1)
  def transition(automated: Boolean = false, exceptionHandler: TransitionExceptionHandler = (e, n) => BlockTransition)(
    fn: S => E
  ): TransitionBehaviour[S, E] = TransitionBehaviour(automated, exceptionHandler, fn)

  lazy val petriNet = {
    val nrOfSteps = sequence.size
    val transitions = sequence.zipWithIndex.map { case (t, index) => t.asTransition(index + 1, eventSourcing) }

    val places = (1 to (nrOfSteps + 1)).map(i => Place[Unit](id = i))
    val tpedges = transitions.zip(places.tail).map { case (t, p) => arc(t, p, 1) }
    val ptedges = places.zip(transitions).map { case (p, t) => arc(p, t, 1) }
    process[S]((tpedges ++ ptedges): _*)
  }
}
