package io.kagera.api.colored

import io.kagera.api._

trait ColoredTokenGame extends TokenGame[Place, Transition, ColoredMarking] {
  this: PetriNet[Place, Transition] =>

  override def enabledParameters(m: ColoredMarking): Map[Transition, Iterable[ColoredMarking]] =
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

  def consumableMarkings(marking: ColoredMarking)(t: Transition): Iterable[ColoredMarking] = {
    val firstEnabled = inMarking(t).map { case (place, count) =>
      place -> marking(place).take(count.toInt)
    }
    Seq(firstEnabled)
  }

  override def enabledTransitions(marking: ColoredMarking): Set[Transition] =
    simple.findEnabledTransitions(this)(marking.multiplicity)
}
