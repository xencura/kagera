package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.colored.ColoredMarking.MarkingData
import io.kagera.api.multiset._

trait ColoredTokenGame extends TokenGame[Place[_], Transition[_, _, _], ColoredMarking] {
  this: PetriNet[Place[_], Transition[_, _, _]] =>

  override def enabledParameters(m: ColoredMarking): Map[Transition[_, _, _], Iterable[ColoredMarking]] =
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

  def consumableMarkings(marking: ColoredMarking)(t: Transition[_, _, _]): Iterable[ColoredMarking] = {
    // TODO this is not the most efficient, should break early when consumable tokens < edge weight
    val consumable = inMarking(t).map { case (place, count) =>
      (place, count, consumeableTokens(marking, place, t))
    }

    // check if any
    if (consumable.exists { case (place, count, tokens) => tokens.multisetSize < count })
      Seq.empty
    else {
      val marking: MarkingData = consumable.map { case (place, count, tokens) =>
        place -> MultiSet.from(tokens.allElements.take(count.toInt))
      }.toMap

      Seq(marking)
    }
  }

  def consumeableTokens[C](marking: ColoredMarking, p: Place[C], t: Transition[_, _, _]): MultiSet[C] = {

    val edge = innerGraph.findPTEdge(p, t).get.label.asInstanceOf[PTEdge[Any]]
    marking.get(p) match {
      case None => MultiSet.empty
      case Some(tokens) => tokens.filter { case (e, count) => edge.filter(e) }
    }
  }

  override def enabledTransitions(marking: ColoredMarking): Set[Transition[_, _, _]] =
    // TODO optimize, no need to process all transitions
    transitions.filter(t => consumableMarkings(marking)(t).nonEmpty)
}
