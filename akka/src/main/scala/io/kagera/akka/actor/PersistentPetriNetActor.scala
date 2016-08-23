package io.kagera.akka.actor

import java.util.UUID

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import io.kagera.akka.actor.PersistentPetriNetActor._
import io.kagera.akka.actor.TransitionEventAdapter.TransitionFiredPersist
import io.kagera.api._
import io.kagera.api.colored._
import shapeless.tag._

import scala.collection._
import scala.language.existentials
import scala.util.{ Failure, Random, Success }

object PersistentPetriNetActor {

  // we don't want to store the consumed token values in this event, just pointers / identifiers
  // how to deterministically assign each token an identifier?
  case object GetState

  case class TransitionFired(
    transition_id: Long,
    time_started: Long,
    time_completed: Long,
    consumed: ColoredMarking,
    produced: ColoredMarking,
    out: Any
  )

  sealed trait TransitionResult

  // response
  case class TransitionFiredSuccessfully[S](
    transition: Long,
    consumed: ColoredMarking,
    produced: ColoredMarking,
    marking: ColoredMarking,
    state: S
  ) extends TransitionResult

  case class TransitionFailed[S](
    transition: Transition[_, _, S],
    consume: ColoredMarking,
    input: Any,
    reason: Throwable
  ) extends TransitionResult

  case class FireTransition(transition_id: Long @@ tags.Id, input: Any)

  case class State[S](marking: ColoredMarking, state: S)

  case class JobCompleted(id: Long)

  case class JobTimedout(id: Long)

  def props[S](id: UUID, process: ColoredPetriNetProcess[S], initialMarking: ColoredMarking, initialState: S) =
    Props(new PersistentPetriNetActor[S](id: UUID, process, initialMarking, initialState))
}

class PersistentPetriNetActor[S](
  id: UUID,
  process: ColoredPetriNetProcess[S],
  initialMarking: ColoredMarking,
  initialState: S
) extends PersistentActor
    with ActorLogging
    with TransitionEventAdapter[S] {

  override def persistenceId: String = s"petrinet-$id"

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

  def transitionStatus(t: Transition[Any, _, S]) = ???

  val runningJobs: mutable.Map[Long, Job] = mutable.Map.empty
  val exceptionState: mutable.Map[Long, (Long, Throwable)] = mutable.Map.empty

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
            step()
            sender() ! response
          }
        case Failure(reason) =>
          log.warning(s"Transition '${job.t}' failed: {}", reason)
          runningJobs -= id
          sender ! TransitionFailed(job.t, job.consume, job.input, reason)
      }

    case e: TransitionFailed[_] =>
      log.warning(s"Transition '${e.transition}' failed: {}", e)
      sender() ! e

    case FireTransition(id, input) => fire(process.getTransitionById(id), input)
  }

  /**
   * Fires the first enabled transition
   */
  def step() = {
    process
      .enabledParameters(availableMarking)
      .view
      .filter { case (t, markings) =>
        t.isManaged
      }
      .headOption
      .foreach { case (t, markings) =>
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

  override def receiveRecover: Receive = { case e: TransitionFiredPersist =>
    applyEvent(readEvent(process, currentMarking, e))
  }
}
