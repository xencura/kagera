package io.kagera.api.colored

import io.kagera.api._

trait ColoredTokenGame extends TokenGame[Place, Transition, ColoredMarking] {
  this: PetriNet[Place, Transition] =>

  override def enabledParameters(m: ColoredMarking): Map[Transition, Iterable[ColoredMarking]] =
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

  def consumableMarkings(marking: ColoredMarking)(t: Transition): Iterable[ColoredMarking] = {

    // TODO this is not the most efficient, should break early when consumable tokens < edge weight
    val consumable = inMarking(t).map { case (place, count) =>
      (place, count, consumeableTokens(marking, place, t))
    }

    // check if any
    if (consumable.exists { case (place, count, tokens) => tokens.size < count })
      Seq.empty
    else
      Seq(consumable.map { case (place, count, tokens) => place -> tokens.take(count.toInt) }.toMap)
  }

  def consumeableTokens(marking: ColoredMarking, p: Place, t: Transition) = {
    val edge = innerGraph.findPTEdge(p, t).get.label.asInstanceOf[PTEdge[Any]]
    marking.get(p) match {
      case None => Seq.empty
      case Some(tokens) =>
        tokens.filter(edge.filter)
    }
  }

  override def enabledTransitions(marking: ColoredMarking): Set[Transition] =
    // TODO optimize, no need to process all transitions
    transitions.filter(t => consumableMarkings(marking)(t).nonEmpty)
}
