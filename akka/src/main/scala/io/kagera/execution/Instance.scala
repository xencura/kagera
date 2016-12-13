package io.kagera.execution

import io.kagera.api._
import io.kagera.api.colored._

import scala.collection.{ Iterable, Map }
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

  // The marking that is already used by running jobs
  lazy val reservedMarking: Marking =
    jobs.map { case (id, job) => job.consume }.reduceOption(_ |+| _).getOrElse(Marking.empty)

  // The marking that is available for new jobs
  lazy val availableMarking: Marking = marking |-| reservedMarking

  def failedJobs: Iterable[ExceptionState] = jobs.values.map(_.failure).flatten

  def isBlockedReason(transitionId: Long): Option[String] = failedJobs
    .map {
      case ExceptionState(`transitionId`, _, reason, _) =>
        Some(
          s"Transition '${process.transitions.getById(transitionId)}' is blocked because it failed previously with: $reason"
        )
      case ExceptionState(tid, _, reason, ExceptionStrategy.Fatal) =>
        Some(s"Transition '${process.transitions.getById(tid)}' caused a Fatal exception")
      case _ => None
    }
    .find(_.isDefined)
    .flatten

  def nextJobId(): Long = Random.nextLong()
}
