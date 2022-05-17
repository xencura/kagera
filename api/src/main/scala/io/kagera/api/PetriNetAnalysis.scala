/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

  def reachable[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], target: MultiSet[P]): Boolean = ???

  def boundedness[P, T](pn: PetriNet[P, T])(m0: MultiSet[P], p: P) = ???

  def liveliness[P, T](pn: PetriNet[P, T])(m: MultiSet[P], t: T) = ???
}
