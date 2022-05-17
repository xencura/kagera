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

import multiset._
import scalax.collection.edge.WLDiEdge

/**
 * Petri net interface.
 *
 * TODO also incorporate the edge types, P -> T and T -> P
 */
trait PetriNet[P, T] {

  /**
   * The scala-graph backing petri net.
   *
   * @return
   */
  def innerGraph: BiPartiteGraph[P, T, WLDiEdge]

  /**
   * The set of places of the petri net
   *
   * @return
   *   The set of places
   */
  def places: Set[P]

  /**
   * The set of transitions of the petri net
   *
   * @return
   *   The set of transitions.
   */
  def transitions: Set[T]

  /**
   * The out-adjecent places of a transition.
   *
   * @param t
   *   transition
   * @return
   */
  def outgoingPlaces(t: T): Set[P]

  /**
   * The out-adjacent transitions of a place.
   *
   * @param p
   *   place
   * @return
   */
  def outgoingTransitions(p: P): Set[T]

  /**
   * The in-adjacent places of a transition.
   *
   * @param t
   *   transition
   * @return
   */
  def incomingPlaces(t: T): Set[P]

  /**
   * The in-adjacent transitions of a place.
   *
   * @param p
   *   place
   * @return
   */
  def incomingTransitions(p: P): Set[T]

  /**
   * Returns the in-marking of a transition. That is; a map of place -> arc weight
   *
   * @param t
   *   transition
   * @return
   */
  def inMarking(t: T): MultiSet[P]

  /**
   * The out marking of a transition. That is; a map of place -> arc weight
   *
   * @param t
   *   transition
   * @return
   */
  def outMarking(t: T): MultiSet[P]

  /**
   * The set of nodes (places + transitions) in the petri net.
   *
   * @return
   *   The set of nodes.
   */
  def nodes: scala.collection.Set[Either[P, T]]

  /**
   * Checks whether a transition is 'enabled' in a certain marking.
   *
   * @param marking
   * @param t
   * @return
   */
  def isEnabledInMarking(marking: MultiSet[P])(t: T): Boolean = marking.isSubSet(inMarking(t))
}
