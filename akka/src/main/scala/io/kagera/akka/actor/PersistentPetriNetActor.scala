package io.kagera.akka.actor

import java.util.UUID

import akka.persistence.PersistentActor
import io.kagera.akka.actor.PersistentPetriNetActor.{ FireTransition, TransitionFired }
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.api.multiset._

object PersistentPetriNetActor {

  // we don't want to store the consumed token values in this event, just pointers / identifiers
  // how to deterministically assign each token an identifier?
  case object GetState

  case class TransitionFired(transition_id: Long, consumed: Set[Long], produced: Map[Long, MultiSet[_]], out: Any)
  case class FireTransition(transition_id: Long, input: Any)
}

class PersistentPetriNetActor[S](
  id: UUID,
  process: ColoredPetriNetProcess[S],
  initialMarking: ColoredMarking,
  initialState: S
) extends PersistentActor {

  override def persistenceId: String = s"petrinet-$id"

  var marking: ColoredMarking = initialMarking
  var state: S = initialState

  def getTransitionById(id: Long): Transition[Any, _, S] =
    process.transitions
      .findById(id)
      .getOrElse { throw new IllegalStateException(s"No transition found with identifier: $id") }
      .asInstanceOf[Transition[Any, _, S]]

  def getPlaceById(id: Long): Place[_] =
    process.places.findById(id).getOrElse { throw new IllegalStateException(s"No place found with identifier: $id") }

  def getById[C](marking: ColoredMarking, id: Iterable[Long]): ColoredMarking = ???

  override def receiveCommand: Receive = { case FireTransition(id, input) =>
    val transition = getTransitionById(id)
    val foo = process.fireTransition(transition)(marking, state, input)

  //      process.fireTransition(transition)(marking, state, input)
  }

  override def receiveRecover: Receive = { case TransitionFired(id, consumed, produced, out) =>
    val transition = getTransitionById(id)
    val consumedMarking: ColoredMarking = getById(marking, consumed)
    val producedMarking: ColoredMarking = ColoredMarking(data = produced.map { case (id, tokens) =>
      getPlaceById(id) -> tokens
    }.toMap)

    //      state = transition.updateState(out)

    marking = marking -- consumedMarking ++ producedMarking
  }
}
