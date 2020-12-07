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
trait Place[Color] extends Node {
  def apply[T <: Color](_tokens: T*): MarkedPlace[Color] = (this, MultiSet(_tokens: _*))
}
