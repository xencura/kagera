package io.kagera.api.colored

object Place {
  def apply[A](id: Long, label: String) = PlaceImpl[A](id, label)
}

/**
 * A Place in a colored petri net.
 *
 * TODO !! A special kind of place where Color == Nothing (or Null?) should exist for which no data should be kept in
 * the marking. This then supports the uncolored or 'normal' petri net place.
 */
trait Place {

  /**
   * The type or 'color' of this place.
   */
  type Color

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
}
