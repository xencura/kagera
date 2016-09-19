package io.kagera.akka.actor

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored._

import scala.collection._
import scala.language.existentials
import scala.util.{ Failure, Random, Success }

object PetriNetProcess {

  // commands
  trait Command

  /**
   * Command to request the next enabled transition to be fired.
   */
  case object Step extends Command

  /**
   * Command to request the current state of the process.
   */
  case object GetState extends Command

  /**
   * Command to fire a specific transition with input.
   */
  case class FireTransition(transition_id: Long, input: Any) extends Command

  // responses
  sealed trait TransitionResult

  /**
   * Response indicating that a transition has fired successfully
   */
  case class TransitionFired[S](transition_id: Long, consumed: Marking, produced: Marking, marking: Marking, state: S)
      extends TransitionResult

  /**
   * Response indicating that a transition has failed.
   */
  case class TransitionFailed(transition_id: Long, consume: Marking, input: Any, reason: Throwable)
      extends TransitionResult

  /**
   * Internal message indicating a job has completed
   */
  protected case class JobCompleted(id: Long)

  /**
   * Response containing the state of the process.
   */
  case class State[S](marking: Marking, state: S)

  /**
   * An event describing the fact that a transition has fired in the petri net process.
   */
  case class TransitionFiredEvent(
    transition_id: Long,
    time_started: Long,
    time_completed: Long,
    consumed: Marking,
    produced: Marking,
    out: Any
  )

  def props[S](process: ExecutablePetriNet[S], initialMarking: Marking, initialState: S) =
    Props(new PetriNetProcess[S](process, initialMarking, initialState))
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetProcess[S](process: ExecutablePetriNet[S], initialMarking: Marking, initialState: S)
    extends PersistentActor
    with ActorLogging
    with PetriNetEventAdapter[S] {

  val processId = context.self.path.name

  override def persistenceId: String = s"process-$processId"

  override implicit val system = context.system

  def currentTime(): Long = System.currentTimeMillis()

  var currentMarking: Marking = initialMarking
  var state: S = initialState

  case class Job(id: Long, t: Transition[Any, _, S], consume: Marking, input: Any, startTime: Long = currentTime()) {
    val result = process.fireTransition(t)(consume, state, input)
  }

  import context.dispatcher

  def nextJobId(): Long = Random.nextLong()

  val runningJobs: mutable.Map[Long, Job] = mutable.Map.empty

  // The marking that is already used by running jobs
  def reservedMarking: Marking =
    runningJobs.map { case (id, job) => job.consume }.reduceOption(_ ++ _).getOrElse(Marking.empty)

  // The marking that is available for new transitions / jobs.
  def availableMarking: Marking = currentMarking -- reservedMarking

  override def receiveCommand = {
    case GetState =>
      sender() ! State[S](currentMarking, state)

    case JobCompleted(id) =>
      val job = runningJobs(id)
      job.result.value.foreach {
        case Success((produced, output)) =>
          val e = TransitionFiredEvent(job.t, job.startTime, currentTime(), job.consume, produced, output)
          persist(writeEvent(e)) { persisted =>
            applyEvent(e)
            log.debug(s"Transition fired ${job.t}")
            val response = TransitionFired[S](job.t, e.consumed, e.produced, currentMarking, state)
            runningJobs -= id
            fireAllEnabledTransitions()
            sender() ! response
          }
        case Failure(reason) =>
          log.warning(s"Transition '${job.t}' failed: {}", reason)
          runningJobs -= id
          sender ! TransitionFailed(job.t, job.consume, job.input, reason)
      }

    case e: TransitionFailed =>
      log.warning(s"Transition '${process.getTransitionById(e.transition_id)}' failed: {}", e)
      sender() ! e

    case FireTransition(id, input) => fire(process.getTransitionById(id), input)
  }

  /**
   * Fires all automated enabled transitions concurrently
   */
  def fireAllEnabledTransitions() = fireAllEnabled(availableMarking)

  def fireAllEnabled(available: Marking): Unit = {
    process
      .enabledParameters(availableMarking)
      .find { case (t, markings) =>
        t.isManaged
      }
      .foreach { case (t, markings) =>
        log.debug(s"Transition $t is automated and enabled: firing")
        val job = fire(t.asInstanceOf[Transition[Any, _, S]], markings.head, ())
        fireAllEnabled(available -- job.consume)
      }
  }

  /**
   * Fires a specific transition with input, computes the marking it should consume
   */
  def fire(transition: Transition[Any, _, S], input: Any): Unit = {
    process.enabledParameters(availableMarking).get(transition) match {
      case None =>
        sender() ! TransitionFailed(
          transition,
          Marking.empty,
          input,
          new IllegalStateException(s"Transition $transition is not enabled")
        )
      case Some(params) => fire(transition, params.head, input)
    }
  }

  /**
   * Fires a specific transition with input & marking
   */
  def fire(transition: Transition[Any, _, S], consume: Marking, input: Any): Job = {
    val job = Job(nextJobId(), transition, consume, input)
    runningJobs += job.id -> job
    val originalSender = sender()
    job.result.onComplete { case _ => self.tell(JobCompleted(job.id), originalSender) }
    job
  }

  def applyEvent: Receive = { case e: TransitionFiredEvent =>
    currentMarking = currentMarking -- e.consumed ++ e.produced
    val t = process.getTransitionById(e.transition_id)
    state = t.updateState(state)(e.out)
  }

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.TransitionFired => applyEvent(readEvent(process, currentMarking, e))
    case RecoveryCompleted => fireAllEnabledTransitions()
  }
}
