package io.kagera.api

import io.kagera.api.ScalaGraph._

import scalax.collection.edge.WLDiEdge

/**
 * Petri net interface.
 *
 * TODO enrich this interface with more convenient functions
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
   * The in marking of a transition.
   *
   * @param t
   * @return
   */
  def inMarking(t: T): Marking[P]

  /**
   * The out marking of a transition.
   *
   * @param t
   * @return
   */
  def outMarking(t: T): Marking[P]

  /**
   * The set of nodes (places + transitions) in the petri net.
   *
   * @return
   *   The set of nodes.
   */
  def nodes: scala.collection.Set[Either[P, T]]
}
