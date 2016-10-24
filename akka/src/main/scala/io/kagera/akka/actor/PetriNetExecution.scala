package io.kagera.akka.actor

import fs2.{ Strategy, Task }
import io.kagera.akka.actor.PetriNetEventSourcing._
import io.kagera.akka.actor.PetriNetProcessProtocol.ProcessState
import io.kagera.api.colored._

import scala.collection.{ Iterable, Map, Set }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

object PetriNetExecution {

  type InstanceState[S, T] = Instance[S] => (Instance[S], T)

  /**
   * Fires a specific transition with input, computes the marking it should consume
   */
  def fireTransition[E, S](transition: Transition[Any, E, S], input: Any): InstanceState[S, Either[Job[S, E], String]] =
    s => {
      s.isBlockedReason(transition.id) match {
        case Some(reason) =>
          (s, Right(reason))
        case None =>
          s.process.enabledParameters(s.availableMarking).get(transition) match {
            case None =>
              (s, Right(s"Not enough consumable tokens"))
            case Some(params) =>
              val (updatedState, job) = fireUnsafe(transition, params.head, input)(s)
              (updatedState, Left(job))
          }
      }
    }

  /**
   * Creates a job for a specific input & marking. Does not do any validation on the parameters
   */
  def fireUnsafe[E, S](transition: Transition[Any, E, S], consume: Marking, input: Any): InstanceState[S, Job[S, E]] =
    s => {
      val job = Job[S, E](s.nextJobId(), s.process, s.state, transition, consume, input)
      val newState = s.copy(jobs = s.jobs + (job.id -> job))
      (newState, job)
    }

  def fireAllEnabledTransitions[S]: InstanceState[S, Set[Job[S, _]]] = s => {
    val enabled = s.process.enabledParameters(s.availableMarking).find { case (t, markings) =>
      t.isAutomated && !s.isBlockedReason(t.id).isDefined
    }

    enabled.headOption
      .map { case (t, markings) =>
        val (newState, job) = fireUnsafe(t.asInstanceOf[Transition[Any, Any, S]], markings.head, ())(s)
        fireAllEnabledTransitions(newState) match { case (state, jobs) => (state, jobs + job) }
      }
      .getOrElse((s, Set.empty[Job[S, _]]))
  }

  /**
   * Executes a job returning a TransitionEvent
   */
  def runJob[S, E](job: Job[S, E])(implicit S: Strategy): Task[TransitionEvent] = {
    val startTime = System.currentTimeMillis()

    job.process
      .fireTransition(job.transition)(job.consume, job.processState, job.input)
      .map { case (produced, out) =>
        TransitionFiredEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          produced,
          out
        )
      }
      .handle { case e: Throwable =>
        val failureCount = job.failureCount + 1
        val failureStrategy = job.transition.exceptionStrategy(e, failureCount)
        TransitionFailedEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          job.input,
          e.getCause.getMessage,
          failureStrategy
        )
      }
      .async
  }

  case class ExceptionState(
    transitionId: Long,
    consecutiveFailureCount: Int,
    failureReason: String,
    failureStrategy: ExceptionStrategy
  )

  case class Job[S, E](
    id: Long,
    process: ExecutablePetriNet[S],
    processState: S,
    transition: Transition[Any, E, S],
    consume: Marking,
    input: Any,
    failure: Option[ExceptionState] = None
  ) {

    lazy val failureCount = failure.map(_.consecutiveFailureCount).getOrElse(0)
  }

  object Instance {
    def uninitialized[S](process: ExecutablePetriNet[S]): Instance[S] =
      Instance[S](process, 0, Marking.empty, null.asInstanceOf[S], Map.empty)
  }

  case class Instance[S](
    process: ExecutablePetriNet[S],
    sequenceNr: BigInt,
    marking: Marking,
    state: S,
    jobs: Map[Long, Job[S, _]]
  ) {

    lazy val processState: ProcessState[S] = ProcessState[S](sequenceNr, marking, state)

    // The marking that is already used by running jobs
    lazy val reservedMarking: Marking =
      jobs.map { case (id, job) => job.consume }.reduceOption(_ ++ _).getOrElse(Marking.empty)

    // The marking that is available for new jobs
    lazy val availableMarking: Marking = marking -- reservedMarking

    def failedJobs: Iterable[ExceptionState] = jobs.values.map(_.failure).flatten

    def isBlockedReason(transitionId: Long): Option[String] = failedJobs
      .map {
        case ExceptionState(`transitionId`, _, reason, _) =>
          Some(
            s"Transition '${process.getTransitionById(transitionId)}' is blocked because it failed previously with: $reason"
          )
        case ExceptionState(tid, _, reason, ExceptionStrategy.Fatal) =>
          Some(s"Transition '${process.getTransitionById(tid)}' caused a Fatal exception")
        case _ => None
      }
      .find(_.isDefined)
      .flatten

    def nextJobId(): Long = Random.nextLong()
  }
}
