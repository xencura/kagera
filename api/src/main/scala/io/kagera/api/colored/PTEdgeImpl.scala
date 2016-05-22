package io.kagera.api.colored

class PTEdgeImpl(override val weight: Long, override val filter: Place => Place#Color => Boolean) extends PTEdge {}
