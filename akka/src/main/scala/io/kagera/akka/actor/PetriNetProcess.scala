package io.kagera.akka.actor

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored._

import scala.collection._
import scala.language.existentials
import scala.util.{ Failure, Random, Success }

object PetriNetProcess {

  // commands
  trait Command

  case object GetState extends Command

  case class FireTransition(transition_id: Long, input: Any)

  // responses
  sealed trait TransitionResult

  case class TransitionFiredSuccessfully[S](
    transition_id: Long,
    consumed: ColoredMarking,
    produced: ColoredMarking,
    marking: ColoredMarking,
    state: S
  ) extends TransitionResult

  case class TransitionFailed(transition_id: Long, consume: ColoredMarking, input: Any, reason: Throwable)
      extends TransitionResult

  case class State[S](marking: ColoredMarking, state: S)

  case class TransitionExceptionState(time: Long, exception: Throwable, exceptionStrategy: ExceptionStrategy)

  // events
  case class TransitionFired(
    transition_id: Long,
    time_started: Long,
    time_completed: Long,
    consumed: ColoredMarking,
    produced: ColoredMarking,
    out: Any
  )

  protected case class JobCompleted(id: Long)

  protected case class JobTimedout(id: Long)

  def props[S](process: ExecutablePetriNet[S], initialMarking: ColoredMarking, initialState: S) =
    Props(new PetriNetProcess[S](process, initialMarking, initialState))
}

class PetriNetProcess[S](process: ExecutablePetriNet[S], initialMarking: ColoredMarking, initialState: S)
    extends PersistentActor
    with ActorLogging
    with PetriNetEventAdapter[S] {

  val id = context.self.path.name

  override def persistenceId: String = s"process-$id"

  override implicit val system = context.system
  def currentTime(): Long = System.currentTimeMillis()

  var currentMarking: ColoredMarking = initialMarking
  var state: S = initialState

  case class Job(
    id: Long,
    t: Transition[Any, _, S],
    consume: ColoredMarking,
    input: Any,
    startTime: Long = currentTime()
  ) {
    val result = process.fireTransition(t)(consume, state, input)
  }

  import context.dispatcher

  def nextJobId(): Long = Random.nextLong()

  val runningJobs: mutable.Map[Long, Job] = mutable.Map.empty

  val exceptionState: mutable.Map[Long, TransitionExceptionState] = mutable.Map.empty

  def isBlocked(t: Transition[Any, _, S]): Option[String] = exceptionState.get(t.id).map {
    case TransitionExceptionState(time, exception, RetryWithDelay(initialDelay, count, fn)) => ""
  }

  // The marking that is already used by running jobs
  def reservedMarking: ColoredMarking =
    runningJobs.map { case (id, job) => job.consume }.reduceOption(_ ++ _).getOrElse(ColoredMarking.empty)
  def availableMarking: ColoredMarking = currentMarking -- reservedMarking

  override def receiveCommand = {
    case GetState =>
      sender() ! State[S](currentMarking, state)

    case JobCompleted(id) =>
      val job = runningJobs(id)
      job.result.value.foreach {
        case Success((produced, output)) =>
          val e = TransitionFired(job.t, job.startTime, currentTime(), job.consume, produced, output)
          persist(writeEvent(e)) { persisted =>
            applyEvent(e)
            log.debug(s"Transition fired ${job.t}")
            val response = TransitionFiredSuccessfully[S](job.t, e.consumed, e.produced, currentMarking, state)
            runningJobs -= id
            tryStep()
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
   * Fires the first enabled transition
   */
  def tryStep() = {
    val enabled = process
      .enabledParameters(availableMarking)
      .view
      .filter { case (t, markings) =>
        t.isManaged
      }
      .headOption

    if (!enabled.isDefined)
      log.debug("Cannot fire an automatic transition, none are enabled")

    enabled.foreach { case (t, markings) =>
      fire(t.asInstanceOf[Transition[Any, _, S]], ())
    }
  }

  /**
   * Fires a specific transition with input
   *
   * @param transition
   * @param input
   * @return
   */
  def fire(transition: Transition[Any, _, S], input: Any): Unit = {
    process.enabledParameters(availableMarking).get(transition) match {
      case None =>
        sender() ! TransitionFailed(
          transition,
          ColoredMarking.empty,
          input,
          new IllegalStateException(s"Transition $transition is not enabled")
        )
      case Some(params) => fire(transition, params.head, input)
    }
  }

  def fire(transition: Transition[Any, _, S], consume: ColoredMarking, input: Any): Unit = {
    log.debug(s"Firing transition: $transition")
    val job = Job(nextJobId(), transition, consume, input)
    runningJobs += job.id -> job
    val originalSender = sender()
    job.result.onComplete { case _ => self.tell(JobCompleted(job.id), originalSender) }
  }

  def applyEvent: Receive = { case e: TransitionFired =>
    currentMarking = currentMarking -- e.consumed ++ e.produced
    val t = process.getTransitionById(e.transition_id)
    state = t.updateState(state)(e.out)
  }

  override def receiveRecover: Receive = { case e: io.kagera.akka.persistence.TransitionFired =>
    applyEvent(readEvent(process, currentMarking, e))
  }
}
