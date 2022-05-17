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

import io.kagera.api._
import io.kagera.api.colored._

import scala.collection.Iterable
import scala.collection.immutable.Map
import scala.util.Random

object Instance {
  def uninitialized[S](process: ExecutablePetriNet[S]): Instance[S] =
    Instance[S](process, 0, Marking.empty, null.asInstanceOf[S], Map.empty)
}

case class Instance[S](
  process: ExecutablePetriNet[S],
  sequenceNr: Long,
  marking: Marking,
  state: S,
  jobs: Map[Long, Job[S, _]]
) {
  def enabledParameters: Map[Transition[_, _, _], Iterable[Marking]] = process.enabledParameters(availableMarking)
  def enabledTransitions: Set[Transition[_, _, _]] = process.enabledTransitions(marking)
  def transitionById(id: Long): Option[Transition[_, _, _]] = process.transitions.findById(id)
  // The marking that is already used by running jobs
  lazy val reservedMarking: Marking =
    jobs.map { case (_, job) => job.consume }.reduceOption(_ |+| _).getOrElse(Marking.empty)

  // The marking that is available for new jobs
  lazy val availableMarking: Marking = marking |-| reservedMarking

  def activeJobs: Iterable[Job[S, _]] = jobs.values.filter(_.isActive)

  def failedJobs: Iterable[ExceptionState] = jobs.values.flatMap(_.failure)

  def isBlockedReason(transitionId: Long): Option[String] = failedJobs
    .map {
      case ExceptionState(`transitionId`, _, reason, _) =>
        Some(s"Transition '${transitionById(transitionId)}' is blocked because it failed previously with: $reason")
      case ExceptionState(tid, _, reason, ExceptionStrategy.Fatal) =>
        Some(s"Transition '${transitionById(tid)}' caused a Fatal exception: $reason")
      case _ => None
    }
    .find(_.isDefined)
    .flatten

  def nextJobId(): Long = Random.nextLong()
}
