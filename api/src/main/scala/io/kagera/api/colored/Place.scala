package io.kagera.api.colored

import io.kagera.api.multiset._

object Place {
  def apply[C](id: Long, label: Option[String] = None): Place[C] = {

    PlaceImpl[C](id, label.getOrElse(s"p$id"))
  }
}

/**
 * A Place in a colored petri net.
 */
trait Place[+Color] {

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

  //def apply(_tokens: Color*): MarkedPlace[Color] = (this, MultiSet[Color](_tokens: _*))
}
