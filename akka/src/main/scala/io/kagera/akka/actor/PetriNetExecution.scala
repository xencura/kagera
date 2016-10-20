package io.kagera.akka.actor

import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.akka.actor.PetriNetProcessProtocol.ProcessState
import io.kagera.api.colored.{ ExceptionStrategy, Marking, Transition, _ }

import scala.collection.{ Iterable, Map, Set }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

object PetriNetExecution {

  case class Job[S, E](
    id: Long,
    process: ExecutablePetriNet[S],
    processState: S,
    transition: Transition[Any, E, S],
    consume: Marking,
    input: Any,
    startTime: Long
  ) {

    var failureCount = 0
    var lastRun: Future[TransitionEvent] = null

    def failure: Option[ExceptionState] = lastRun match {
      case null => None
      case future if future.isCompleted =>
        future.value.get.get match {
          case e: TransitionFailedEvent => Some(ExceptionState(transition.id, e.failureReason, e.exceptionStrategy))
          case _ => None
        }
      case _ => None
    }

    def run()(implicit ec: ExecutionContext): Future[TransitionEvent] = {

      lastRun = process
        .fireTransition(transition)(consume, processState, input)
        .map { case (produced, out) =>
          TransitionFiredEvent(id, transition.id, startTime, System.currentTimeMillis(), consume, produced, out)
        }
        .recover { case e: Throwable =>
          failureCount += 1
          TransitionFailedEvent(
            id,
            transition.id,
            startTime,
            System.currentTimeMillis(),
            consume,
            input,
            e.getCause.getMessage,
            transition.exceptionStrategy(e, failureCount)
          )
        }
      lastRun
    }
  }

  case class ExceptionState(transitionId: Long, failureReason: String, failureStrategy: ExceptionStrategy)

  case class ExecutionState[S](
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

    // The marking that is available for new transitions / jobs.
    lazy val availableMarking: Marking = marking -- reservedMarking

    def failedJobs: Iterable[ExceptionState] = jobs.values.map(_.failure).flatten

    def isBlockedReason(transitionId: Long): Option[String] = failedJobs
      .map {
        case ExceptionState(`transitionId`, reason, _) =>
          Some(
            s"Transition '${process.getTransitionById(transitionId)}' is blocked because it failed previously with: $reason"
          )
        case ExceptionState(tid, reason, ExceptionStrategy.Fatal) =>
          Some(s"Transition '${process.getTransitionById(tid)}' caused a Fatal exception")
        case _ => None
      }
      .find(_.isDefined)
      .flatten

    protected def currentTime(): Long = System.currentTimeMillis()

    protected def nextJobId(): Long = Random.nextLong()

    def apply(e: TransitionFiredEvent) = {
      val t = process.getTransitionById(e.transitionId)
      val newState = t.updateState(state)(e.out)
      copy(
        sequenceNr = this.sequenceNr + 1,
        marking = this.marking -- e.consumed ++ e.produced,
        state = newState,
        jobs = this.jobs - e.jobId
      )
    }

    /**
     * Fires a specific transition with input, computes the marking it should consume
     */
    def fireTransition[E](
      transition: Transition[Any, E, S],
      input: Any
    ): (ExecutionState[S], Either[Job[S, E], String]) = {
      isBlockedReason(transition.id) match {
        case Some(reason) =>
          (this, Right(reason))
        case None =>
          process.enabledParameters(availableMarking).get(transition) match {
            case None =>
              (this, Right(s"Not enough consumable tokens"))
            case Some(params) =>
              val (state, job) = createJob(transition, params.head, input)
              (state, Left(job))
          }
      }
    }

    /**
     * Creates a job for a specific input & marking. Does not do any validation on the parameters
     */
    protected def createJob[E](
      transition: Transition[Any, E, S],
      consume: Marking,
      input: Any
    ): (ExecutionState[S], Job[S, E]) = {
      val job = Job[S, E](nextJobId(), process, state, transition, consume, input, currentTime())
      val newState = copy(jobs = this.jobs + (job.id -> job))
      (newState, job)
    }

    def fireAllEnabledTransitions(): (ExecutionState[S], Set[Job[S, _]]) = {
      val enabled = process.enabledParameters(availableMarking).find { case (t, markings) =>
        t.isAutomated && !isBlockedReason(t).isDefined
      }

      enabled.headOption
        .map { case (t, markings) =>
          val (newState, job) = createJob(t.asInstanceOf[Transition[Any, Any, S]], markings.head, ())
          newState.fireAllEnabledTransitions() match {
            case (state, jobs) => (state, jobs + job)
          }
        }
        .getOrElse((this, Set.empty[Job[S, _]]))
    }
  }
}
