package io.kagera.api.colored

class PTEdgeImpl(override val weight: Long, override val filter: Place => Place#Color => Boolean) extends PTEdge {

  def withFilter(newFilter: Place => Place#Color => Boolean) = new PTEdgeImpl(weight, newFilter)
}
