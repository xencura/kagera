package io.kagera.api.colored

import io.kagera.api.multiset.MultiSet

object MarkedPlace {
  def apply[C](p: Place[C], t: MultiSet[C]): MarkedPlace[C] = new MarkedPlace[C] {
    override val place = p
    override val tokens = t
  }
}

trait MarkedPlace[Color] {
  val place: Place[Color]
  val tokens: MultiSet[Color]
}
