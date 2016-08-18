package io.kagera.akka.actor

import java.util.UUID

import akka.actor.Status.Failure
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.persistence.PersistentActor
import io.kagera.akka.actor.PersistentPetriNetActor._
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.api.multiset._
import akka.pattern.pipe
import shapeless.tag._

import scala.collection._
import scala.language.existentials

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

  case class TransitionFailed[S](transition: Transition[_, _, S], reason: Throwable)

  // response
  case class TransitionFiredResponse[S](
    transition: Long,
    consumed: ColoredMarking,
    produced: ColoredMarking,
    marking: ColoredMarking,
    state: S
  )

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
  var availableMarking: ColoredMarking = initialMarking
  var state: S = initialState

  import context.dispatcher

  override def receiveCommand = {
    case GetState =>
      sender() ! State[S](currentMarking, state)

    case e: TransitionFired[_] =>
      persist(writeEvent(e)) { persisted =>
        applyEvent(e)
        val response = TransitionFiredResponse[S](e.transition, e.consumed, e.produced, currentMarking, state)
        sender() ! response
        step()
      }
    case e: TransitionFailed[_] =>
      log.info(s"received transition falied: ${e.transition}")
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
        sender() ! TransitionFailed(transition, new IllegalArgumentException(s"Transition $transition is not enabled"))
      case Some(params) => fire(transition, params.head, input)
    }
  }

  def fire(transition: Transition[Any, _, S], consume: ColoredMarking, input: Any): Unit = {

    availableMarking --= consume

    val futureResult = process
      .fireTransition(transition)(consume, state, input)
      .map { case (produced, output) =>
        TransitionFired(transition, consume, produced, output)
      }
      .recover { case e: Throwable =>
        TransitionFailed(transition, e)
      }

    futureResult.pipeTo(self)(sender())
  }

  def applyEvent: Receive = { case e: TransitionFired[_] =>
    currentMarking = currentMarking -- e.consumed ++ e.produced
    availableMarking ++= e.produced
    state = e.transition.asInstanceOf[Transition[_, Any, S]].updateState(state)(e.out)
  }

  override def receiveRecover: Receive = { case e: TransitionFiredPersist =>
    applyEvent(readEvent(process, currentMarking, e))
  }
}
