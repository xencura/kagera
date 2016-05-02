package io.kagera.api

trait TokenGame[P, T, M] {

  this: PetriNet[P, T] =>

  def enabledParameters(m: M): Map[T, Iterable[M]] = {
    // inefficient, fix
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap
  }

  def consumableMarkings(m: M)(t: T): Iterable[M]

  // horribly inefficient, fix
  def isEnabled(marking: M)(t: T): Boolean = enabledTransitions(marking).contains(t)

  def enabledTransitions(marking: M): Set[T]
}
