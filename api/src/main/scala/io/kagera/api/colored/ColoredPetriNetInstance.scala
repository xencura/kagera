package io.kagera.api.colored

import io.kagera.api.{ PetriNetInstance, PetriNetProcess }

import scala.concurrent.Future

class ColoredPetriNetInstance(
  process: PetriNetProcess[Place, Transition, ColoredMarking],
  initialMarking: ColoredMarking
)(implicit executor: scala.concurrent.ExecutionContext)
    extends PetriNetInstance[Place, Transition, ColoredMarking] {

  override def topology = process

  var currentMarking: ColoredMarking = initialMarking

  var accumulated: ColoredMarking = initialMarking

  override def marking: ColoredMarking = currentMarking

  override def accumulatedMarking: ColoredMarking = accumulated

  override def fireTransition(t: Transition, data: Option[Any]): Future[ColoredMarking] = {
    process.fireTransition(currentMarking)(t, data).map(applyChange(t)).flatMap(stepManagedRecursive)
  }

  def applyChange(t: Transition)(newMarking: ColoredMarking): ColoredMarking = {
    val newTokens = newMarking remove currentMarking
    println(s"$t fired, produced: $newTokens")
    accumulated = accumulated produce newTokens
    println(s"accumulated marking: $accumulated")
    currentMarking = newMarking
    newMarking
  }

  // this is very dangerous hack, could go into an infinite recursive loop
  def stepManagedRecursive(marking: ColoredMarking): Future[ColoredMarking] = {
    process
      .enabledTransitions(marking)
      .find(_.isManaged)
      .map { t =>
        process.fireTransition(marking)(t, None).map(applyChange(t)).flatMap(stepManagedRecursive)
      }
      .getOrElse(Future.successful(marking))
  }

  override def step(): Future[ColoredMarking] = ???
}
