package io.kagera.api.colored

class PTEdgeImpl[T](override val weight: Long, override val filter: T => Boolean) extends PTEdge[T] {

  def withFilter(newFilter: T => Boolean) = new PTEdgeImpl[T](weight, newFilter)
}
