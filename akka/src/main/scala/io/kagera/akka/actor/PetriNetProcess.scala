package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.pattern.pipe
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.akka.actor.PetriNetProcessProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored._

import scala.collection._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.existentials
import scala.util.Random

object PetriNetProcess {

  def props[S](process: ExecutablePetriNet[S], initialStateProvider: String => (Marking, S)): Props =
    Props(new PetriNetProcess[S](process, initialStateProvider))

  def props[S](process: ExecutablePetriNet[S], initialMarking: Marking, initialState: S): Props =
    props(process, id => (initialMarking, initialState))

  case class Job[S](
    id: Long,
    process: ExecutablePetriNet[S],
    processState: S,
    transition: Transition[Any, _, S],
    consume: Marking,
    input: Any,
    startTime: Long
  ) {

    var failureCount = 0

    def run()(implicit ec: ExecutionContext) = process
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
          transition.exceptionStrategy(e, failureCount)
        )
      }
  }

  case class ExceptionState(consumed: Marking, exceptionStrategy: ExceptionStrategy, consecutiveFailureCount: Int)

  protected case class JobCompleted(id: Long)

  protected case class ExecutionState[S](
    process: ExecutablePetriNet[S],
    sequenceNr: BigInt,
    marking: Marking,
    state: S,
    jobs: Map[Long, Job[S]]
  ) {

    lazy val processState: ProcessState[S] = ProcessState[S](sequenceNr, marking, state)

    // The marking that is already used by running jobs
    lazy val reservedMarking: Marking =
      jobs.map { case (id, job) => job.consume }.reduceOption(_ ++ _).getOrElse(Marking.empty)

    // The marking that is available for new transitions / jobs.
    lazy val availableMarking: Marking = marking -- reservedMarking

    def isBlocked(transition_id: Long) =
      false //failedJobs.get(transition_id).isDefined || failedJobs.values.exists(_ == Fatal)

    protected def currentTime(): Long = System.currentTimeMillis()

    protected def nextJobId(): Long = Random.nextLong()

    def apply(e: TransitionFiredEvent) = {
      val t = process.getTransitionById(e.transition_id)
      val newState = t.updateState(state)(e.out)
      copy(
        sequenceNr = this.sequenceNr + 1,
        marking = this.marking -- e.consumed ++ e.produced,
        state = newState,
        jobs = this.jobs - e.job_id
      )
    }

    /**
     * Fires a specific transition with input, computes the marking it should consume
     */
    def fireTransition(transition: Transition[Any, _, S], input: Any): (ExecutionState[S], Either[Job[S], String]) = {
      if (isBlocked(transition))
        (this, Right(s"Blocked"))
      else
        process.enabledParameters(availableMarking).get(transition) match {
          case None =>
            (this, Right(s"Not enough consumable tokens"))
          case Some(params) =>
            val (state, job) = createJob(transition, params.head, input)
            (state, Left(job))
        }
    }

    /**
     * Creates a job for a specific input & marking. Does not do any validation on the parameters
     */
    protected def createJob(
      transition: Transition[Any, _, S],
      consume: Marking,
      input: Any
    ): (ExecutionState[S], Job[S]) = {
      val job = Job[S](nextJobId(), process, state, transition, consume, input, currentTime())
      val newState = copy(jobs = this.jobs + (job.id -> job))
      (newState, job)
    }

    def fireAllEnabledTransitions(): (ExecutionState[S], Set[Job[S]]) = {
      val enabled = process.enabledParameters(availableMarking).find { case (t, markings) =>
        t.isAutomated && !isBlocked(t)
      }

      enabled.headOption
        .map { case (t, markings) =>
          val (newState, job) = createJob(t.asInstanceOf[Transition[Any, _, S]], markings.head, ())
          newState.fireAllEnabledTransitions() match {
            case (state, jobs) => (state, jobs + job)
          }
        }
        .getOrElse((this, Set.empty[Job[S]]))
    }
  }
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetProcess[S](process: ExecutablePetriNet[S], initialStateFn: String => (Marking, S))
    extends PersistentActor
    with ActorLogging
    with PetriNetEventAdapter[S] {

  val processId = context.self.path.name

  override def persistenceId: String = s"process-$processId"

  override implicit val system = context.system

  def currentTime(): Long = System.currentTimeMillis()

  import context.dispatcher

  val (initialMarking, initialProcessState) = initialStateFn(processId)
  val initialState = ExecutionState[S](process, 1, initialMarking, initialProcessState, Map.empty)

  override def receiveCommand = running(initialState)

  def running(state: ExecutionState[S]): Receive = {
    case GetState =>
      sender() ! state.processState

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      persist(writeEvent(e)) { persisted =>
        log.debug(s"Transition fired ${transitionId}")
        val (newState, jobs) = state.apply(e).fireAllEnabledTransitions()

        jobs.foreach(job => executeJob(job, sender()))

        context become running(newState)
        sender() ! TransitionFired[S](transitionId, e.consumed, e.produced, newState.marking, newState.state)
      }

    case e @ TransitionFailedEvent(
          jobId,
          transitionId,
          timeStarted,
          timeFailed,
          consume,
          input,
          strategy @ RetryWithDelay(delay)
        ) =>
      log.warning(s"Transition '${transitionId}' failed: {}")
      log.info(s"Scheduling a retry of transition ${transitionId} in $delay milliseconds")

      val originalSender = sender()
      system.scheduler.scheduleOnce(delay milliseconds) { executeJob(state.jobs(jobId), originalSender) }

      sender() ! TransitionFailed(transitionId, consume, input, "", strategy)

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, strategy) =>
      log.warning(s"Transition '${transitionId}' failed: {}")
      sender() ! TransitionFailed(transitionId, consume, input, "", strategy)

    case FireTransition(id, input, _) =>
      log.debug(s"Received message to fire transition $id with input: $input")

      process.findTransitionById(id) match {
        case Some(transition) =>
          state.fireTransition(transition, input) match {
            case (_, Right(notEnabledReason)) =>
              sender() ! TransitionNotEnabled(transition, notEnabledReason)
            case (newState, Left(job)) =>
              executeJob(job, sender())
              context become running(newState)
          }
        case None =>
          val msg = s"No transition exists with id: $id"
          sender() ! TransitionNotEnabled(id, msg)
          log.warning(msg)
      }
  }

  def executeJob(job: Job[S], originalSender: ActorRef): Unit = job.run().pipeTo(context.self)(originalSender)

  def applyEvent(state: ExecutionState[S]): Any => ExecutionState[S] = event =>
    event match {
      case e: TransitionFiredEvent => state.apply(e)
    }

  private var recoveringState: ExecutionState[S] = initialState

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.TransitionFired =>
      recoveringState = applyEvent(recoveringState)(readEvent(process, recoveringState.marking, e))
    case RecoveryCompleted =>
      context.become(running(recoveringState))
  }
}
