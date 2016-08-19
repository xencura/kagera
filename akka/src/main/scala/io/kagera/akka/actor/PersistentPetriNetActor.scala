package io.kagera.akka.actor

import java.util.UUID

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import io.kagera.akka.actor.PersistentPetriNetActor._
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.api.multiset._
import shapeless.tag._

import scala.collection._
import scala.language.existentials
import scala.util.{ Failure, Random, Success }

object PersistentPetriNetActor {

  type MarkingIndex = Map[Long, MultiSet[Int]]

  // we don't want to store the consumed token values in this event, just pointers / identifiers
  // how to deterministically assign each token an identifier?
  case object GetState

  /**
   * TODO The accumulated state should be kept in a PersistentView
   */
  case object GetAccumulatedState

  // persist model
  protected case class TransitionFiredPersist(
    transition_id: Long,
    consumed: MarkingIndex,
    produced: Map[Long, MultiSet[_]],
    out: Any
  )

  protected case class TransitionFired[S](
    transition: Transition[_, _, S],
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

  implicit class ColoredMarkingFns(marking: ColoredMarking) {
    def indexed: Map[Long, MultiSet[Int]] = marking.data.map { case (place, tokens) =>
      place.id -> tokens.map { case (value, count) =>
        tokenIdentifier(place)(value) -> count
      }
    }
  }

  implicit class MarkingIndexFns(indexedMarking: MarkingIndex) {
    def realizeFrom(marking: ColoredMarking): ColoredMarking = {
      indexedMarking.map { case (pid, values) =>
        val place = marking.markedPlaces.getById(pid)
        val tokens = values.map { case (id, count) =>
          val value = marking(place).keySet.find(e => tokenIdentifier(place)(e) == id).get
          value -> count
        }

        place -> tokens
      }.toMarking
    }
  }

  // this approach is fragile, the function cannot change ever or recovery breaks
  // a more robust alternative is to generate the ids and persist them
  def tokenIdentifier[C](p: Place[C]): Any => Int = obj => hashCodeOf[Any](obj)

  def hashCodeOf[T](e: T): Int = {
    if (e == null)
      -1
    else
      e.hashCode()
  }

  implicit class ProcessFns[S](process: ColoredPetriNetProcess[S]) {

    def getTransitionById(id: Long): Transition[Any, Any, S] =
      process.transitions.getById(id).asInstanceOf[Transition[Any, Any, S]]
  }

  /**
   * Translates to/from the persist and internal event model
   *
   * @tparam S
   */
  trait TransitionEventAdapter[S] {
    def writeEvent(e: TransitionFired[_]): TransitionFiredPersist = {
      val consumedIndex: Map[Long, MultiSet[Int]] = e.consumed.indexed
      val produceIndex: Map[Long, MultiSet[_]] = e.produced.data.map { case (place, tokens) => place.id -> tokens }.toMap

      TransitionFiredPersist(e.transition, consumedIndex, produceIndex, e.out)
    }

    def readEvent(
      process: ColoredPetriNetProcess[S],
      currentMarking: ColoredMarking,
      e: TransitionFiredPersist
    ): TransitionFired[S] = {
      val transition = process.getTransitionById(e.transition_id)
      val consumed = e.consumed.realizeFrom(currentMarking)
      val produced = ColoredMarking(data = e.produced.map { case (id, tokens) =>
        process.places.getById(id) -> tokens
      }.toMap)
      TransitionFired(transition, consumed, produced, e.out)
    }
  }

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

  var currentMarking: ColoredMarking = initialMarking
  var state: S = initialState

  case class Job(
    id: Long,
    t: Transition[Any, _, S],
    consume: ColoredMarking,
    input: Any,
    startTime: Long = System.currentTimeMillis()
  ) {
    val result = process.fireTransition(t)(consume, state, input)
  }

  import context.dispatcher

  def nextJobId(): Long = Random.nextLong()
  val runningJobs: mutable.Map[Long, Job] = mutable.Map.empty

  def reservedMarking =
    runningJobs.map { case (id, job) => job.consume }.reduceOption(_ ++ _).getOrElse(ColoredMarking.empty)
  def availableMarking = currentMarking -- reservedMarking

  override def receiveCommand = {
    case GetState =>
      sender() ! State[S](currentMarking, state)

    case JobCompleted(id) =>
      val job = runningJobs(id)
      job.result.value.foreach {
        case Success((produced, output)) =>
          val e = TransitionFired(job.t, job.consume, produced, output)
          persist(writeEvent(e)) { persisted =>
            applyEvent(e)
            log.debug(s"Transition fired ${e.transition}")
            val response = TransitionFiredSuccessfully[S](e.transition, e.consumed, e.produced, currentMarking, state)
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

  def applyEvent: Receive = { case e: TransitionFired[_] =>
    currentMarking = currentMarking -- e.consumed ++ e.produced
    state = e.transition.asInstanceOf[Transition[_, Any, S]].updateState(state)(e.out)
  }

  override def receiveRecover: Receive = { case e: TransitionFiredPersist =>
    applyEvent(readEvent(process, currentMarking, e))
  }
}
