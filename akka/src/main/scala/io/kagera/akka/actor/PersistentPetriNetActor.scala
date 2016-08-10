package io.kagera.akka.actor

import java.util.UUID

import akka.actor.ActorRef
import akka.persistence.PersistentActor
import io.kagera.akka.actor.PersistentPetriNetActor._
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.api.multiset._
import akka.pattern.pipe

object PersistentPetriNetActor {

  type MarkingIndex = Map[Long, MultiSet[Int]]

  // we don't want to store the consumed token values in this event, just pointers / identifiers
  // how to deterministically assign each token an identifier?
  case object GetState

  case class TransitionFired(
    transition_id: Long,
    consumed: Map[Long, MultiSet[Int]],
    produced: Map[Long, MultiSet[_]],
    out: Any
  )

  case class FireTransition(transition_id: Long, input: Any)

  implicit class ColoredMarkingFns(marking: ColoredMarking) {
    def indexed: Map[Long, MultiSet[Int]] = marking.data.map { case (p, tokens) =>
      p.id -> tokens.map { case (value, count) =>
        hashCodeOf(value) -> count
      }
    }
  }

  implicit class MarkingIndexFns(indexedMarking: MarkingIndex) {
    def realizeFrom(marking: ColoredMarking): ColoredMarking = {
      val data: Map[Place[_], MultiSet[_]] = indexedMarking.map { case (pid, values) =>
        val place = marking.markedPlaces.findById(pid).get
        val tokens = values.map { case (id, count) =>
          val value = marking(place).keySet.find(e => hashCodeOf(e) == id).get
          value -> count
        }

        place -> tokens
      }.toMap

      ColoredMarking(data)
    }
  }

  def hashCodeOf[T](e: T): Int = {
    if (e == null)
      -1
    else
      e.hashCode()
  }
}

class PersistentPetriNetActor[S](
  id: UUID,
  process: ColoredPetriNetProcess[S],
  initialMarking: ColoredMarking,
  initialState: S
) extends PersistentActor {

  override def persistenceId: String = s"petrinet-$id"

  var availableMarking: ColoredMarking = initialMarking
  var reserved: Map[Transition[_, _, _], ColoredMarking] = Map.empty
  var currentMarking: ColoredMarking = initialMarking
  var state: S = initialState

  import context.dispatcher

  def getTransitionById(id: Long): Transition[Any, _, S] =
    process.transitions
      .findById(id)
      .getOrElse { throw new IllegalStateException(s"No transition found with identifier: $id") }
      .asInstanceOf[Transition[Any, _, S]]

  def getPlaceById(id: Long): Place[_] =
    process.places.findById(id).getOrElse { throw new IllegalStateException(s"No place found with identifier: $id") }

  def getById[C](marking: ColoredMarking, id: Iterable[Long]): ColoredMarking = ???

  override def receiveCommand: Receive = {
    case GetState =>
      sender() ! currentMarking

    case (originalSender: ActorRef, e: TransitionFired) =>
      persist(e) { persisted =>
        applyEvent(e)
        originalSender ! currentMarking
      }

    case FireTransition(id, input) =>
      val transition = getTransitionById(id)
      val originalSender = sender()

      val futureResult = process.enabledParameters(availableMarking).get(transition) match {
        case None => throw new IllegalArgumentException(s"Transition $transition is not enabled")
        case Some(params) =>
          val consume = params.head
          process.fireTransition(transition)(consume, state, input).map { case (produced, output) =>
            val transitionId = transition.id
            val consumedIndex: Map[Long, MultiSet[Int]] = consume.indexed
            val produceIndex: Map[Long, MultiSet[_]] = produced.data.map { case (place, tokens) =>
              place.id -> tokens
            }.toMap

            originalSender -> TransitionFired(transitionId, consumedIndex, produceIndex, output)
          }
      }

      futureResult.pipeTo(self)
  }

  def applyEvent: Receive = { case TransitionFired(id, consumed, produced, out) =>
    println("applying event")

    val transition = getTransitionById(id)
    val consumedMarking = consumed.realizeFrom(currentMarking)
    val producedMarking = ColoredMarking(data = produced.map { case (id, tokens) => getPlaceById(id) -> tokens }.toMap)

    //      state = transition.updateState(out)
    currentMarking = currentMarking -- consumedMarking ++ producedMarking
  }

  override def receiveRecover: Receive = applyEvent
}
