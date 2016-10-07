package io.kagera.api

import io.kagera.api.multiset._

object PetriNetAnalysis {

  def reachabilityGraph[P, T](pn: PetriNet[P, T])(m0: MultiSet[P]) = {

    // find the enabled transitions from m0
    val reachableMarkings = enabled(pn)(m0).map { t =>
      // compute the marking after t has fired
      val mt = m0
        .multisetDifference(pn.inMarking(t))
        .multisetSum(pn.outMarking(t))

      t -> mt
    }
  }

  /**
   * Given a petri net and a marking, finds the enabled transitions in that marking.
   */
  def enabled[P, T](pn: PetriNet[P, T])(m0: MultiSet[P]): Set[T] = {

    // transitions without input may always fire
    val c = pn.transitions.filter(t => pn.incomingPlaces(t).isEmpty)

    val outT = m0.keys
      .map(pn.outgoingTransitions)
      .reduceOption(_ ++ _)
      .getOrElse(Set.empty)
      .filter(t => m0.isSubSet(pn.inMarking(t)))

    c ++ outT
  }

  // should check if each place is 1 bounded
  def is1Safe[P, T](pn: PetriNet[P, T])(m0: MultiSet[P]): Boolean = ???

  def reachable[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], target: Map[P, Long]): Boolean = ???

  def boundedness[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], p: P) = ???

  def liveliness[P, T](pn: PetriNet[P, T])(m: MultiSet[P], t: T) = ???
}
