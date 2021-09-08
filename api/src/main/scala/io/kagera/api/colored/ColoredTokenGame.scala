package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.multiset._

trait ColoredTokenGame[T] extends TokenGame[Place[_], T, Marking] {
  this: ColoredPetriNet[T] =>

  override def enabledParameters(m: Marking): Map[T, Iterable[Marking]] =
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

  def consumableMarkings(marking: Marking)(t: T): Iterable[Marking] = {
    // TODO this is not the most efficient, should break early when consumable tokens < edge weight
    val consumable = inMarking(t).map { case (place, count) =>
      (place, count, consumableTokens(marking, place, t))
    }

    // check if any
    if (consumable.exists { case (place, count, tokens) => tokens.multisetSize < count })
      Seq.empty
    else {
      val consume = consumable.map { case (place, count, tokens) =>
        place -> MultiSet.from(tokens.allElements.take(count))
      }.toMarking

      // TODO lazily compute all permutations instead of only providing the first result
      Seq(consume)
    }
  }

  def consumableTokens[C](marking: Marking, p: Place[C], t: T): MultiSet[C] = {

    val pn = this
    val edge = pn.getEdge(p, t).get

    marking.get(p) match {
      case None => MultiSet.empty
      case Some(tokens) => tokens.filter { case (e, count) => edge.filter(e) }
    }
  }

  // TODO optimize, no need to process all transitions
  override def enabledTransitions(marking: Marking): Set[T] =
    transitions.filter(t => consumableMarkings(marking)(t).nonEmpty)
}
