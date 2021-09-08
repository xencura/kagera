package io.kagera.api.colored

import io.kagera.api.HMap
import io.kagera.api.multiset._

object Marking {

  type MarkingData = Map[Place[_], MultiSet[_]]

  def empty: Marking = HMap[Place, MultiSet](Map.empty)

  def apply[A](m1: MarkedPlace[A]): Marking = {
    HMap[Place, MultiSet](Map(m1): MarkingData)
  }

  def apply[A, B](m1: MarkedPlace[A], m2: MarkedPlace[B]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2): MarkingData)
  }

  def apply[A, B, C](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3): MarkingData)
  }

  def apply[A, B, C, D](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C], m4: MarkedPlace[D]): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3, m4): MarkingData)
  }

  def apply[A, B, C, D, E](
    m1: MarkedPlace[A],
    m2: MarkedPlace[B],
    m3: MarkedPlace[C],
    m4: MarkedPlace[D],
    m5: MarkedPlace[E]
  ): Marking = {
    HMap[Place, MultiSet](Map(m1, m2, m3, m4, m5): MarkingData)
  }
}
