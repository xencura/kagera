package io.kagera.api.colored

case class PlaceImpl[C](override val id: Long, override val label: String) extends Place[C] {

  override def toString = label
}
