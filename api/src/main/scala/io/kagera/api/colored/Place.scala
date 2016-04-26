package io.kagera.api.colored

object Place {
  def apply[A](id: Long, label: String) = PlaceImpl[A](id, label)
}

trait Place {

  type Color

  def id: Long
  def label: String

  override def toString = label
}
