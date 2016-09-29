package io.kagera.api

import multiset._

object PetriNetAnalysis {

  // should check if each place is 1 bounded
  def is1Safe[P, T](pn: PetriNet[P, T])(m0: MultiSet[P]): Boolean = ???

  def reachable[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], target: Map[P, Long]): Boolean = ???

  def boundedness[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], p: P) = ???

  def liveliness[P, T](pn: PetriNet[P, T])(m: MultiSet[P], t: T) = ???
}
