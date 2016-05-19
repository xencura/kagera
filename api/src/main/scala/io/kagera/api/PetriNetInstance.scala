package io.kagera.api

import scala.concurrent.Future

trait PetriNetInstance[P, T, M] {

  /**
   * The topology of the instance.
   *
   * @return
   */
  def topology: PetriNet[P, T]

  /**
   * The current marking of the petri net instance
   *
   * @return
   */
  def marking: M

  /**
   * The historic marking which contains all tokens (consumed or not)
   *
   * @return
   */
  def accumulatedMarking: M

  /**
   * Will attempt to execute the next enabled transition.
   *
   * @return
   */
  def step(): Future[M]

  /**
   * Will attempt to execute a specific transition.
   *
   * @param t
   *   The desired transition.
   * @return
   */
  def fireTransition(t: T, data: Option[Any] = None): Future[M]
}
