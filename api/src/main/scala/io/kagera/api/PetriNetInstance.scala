package io.kagera.api

import scala.concurrent.Future

trait PetriNetInstance[P, T, M] {

  /**
   * The current marking of the petri net instance
   *
   * @return
   */
  def marking: M

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
