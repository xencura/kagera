package io.kagera.api

import io.kagera.api.multiset.MultiSet

import scalaz.syntax.std.boolean._

package object simple {

  def isSubMarking[P](marking: MultiSet[P], other: MultiSet[P]): Boolean =
    !other.exists { case (place, count) =>
      marking.get(place) match {
        case None => true
        case Some(n) if n < count => true
        case _ => false
      }
    }

  def findEnabledTransitions[P, T](pn: PetriNet[P, T])(marking: MultiSet[P]): Set[T] = {

    /**
     * Inefficient way of doing this, we don't need to check every transition.
     */
    pn.transitions.filter(t => isSubMarking(marking, pn.inMarking(t)))
  }

  trait SimpleTokenGame[P, T] extends TokenGame[P, T, MultiSet[P]] {
    this: PetriNet[P, T] =>

    override def consumableMarkings(m: MultiSet[P])(t: T): Iterable[MultiSet[P]] = {
      // for uncolored markings there is only 1 consumable marking per transition
      val in = inMarking(t)
      isSubMarking(m, in).option(in)
    }

    override def enabledTransitions(marking: MultiSet[P]): Set[T] = findEnabledTransitions[P, T](this)(marking)
  }
}
