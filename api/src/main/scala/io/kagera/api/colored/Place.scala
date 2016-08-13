package io.kagera.api.colored

import io.kagera.api.multiset._

object Place {
  def apply[C](id: Long, label: String): Place[C] = PlaceImpl[C](id, label)
}

/**
 * A Place in a colored petri net.
 */
trait Place[Color] {

  /**
   * The unique identifier of this place.
   *
   * @return
   *   A unique identifier.
   */
  def id: Long

  /**
   * A human readable label of this place.
   *
   * @return
   *   The label.
   */
  def label: String

  def apply[T <: Color](_tokens: T*): MarkedPlace[Color] = (this, MultiSet(_tokens: _*))
}
