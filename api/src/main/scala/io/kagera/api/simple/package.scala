package io.kagera.api

import io.kagera.api.multiset._

package object simple {

  def findEnabledTransitions[P, T](pn: PetriNet[P, T])(marking: MultiSet[P]): Set[T] = {

    /**
     * Inefficient way of doing this, we don't need to check every transition in the petri net.
     */
    pn.transitions.filter(t => marking.isSubSet(pn.inMarking(t)))
  }

  // should check if each place is 1 bounded
  def is1Safe[P, T](pn: PetriNet[P, T])(m0: MultiSet[P]): Boolean = ???

  def reachable[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], target: Map[P, Long]): Boolean = ???

  def boundedness[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], p: P) = ???

  def liveliness[P, T](pn: PetriNet[P, T])(m: MultiSet[P], t: T) = ???

  trait SimpleTokenGame[P, T] extends TokenGame[P, T, MultiSet[P]] {
    this: PetriNet[P, T] =>

    override def consumableMarkings(m: MultiSet[P])(t: T): Iterable[MultiSet[P]] = {
      // for uncolored markings there is only 1 consumable marking per transition
      val in = inMarking(t)

      m.isSubSet(in).option(in)
    }

    override def enabledTransitions(marking: MultiSet[P]): Set[T] = findEnabledTransitions[P, T](this)(marking)
  }
}
