package io.kagera.api.colored

import io.kagera.api.colored.Marking.MarkingData
import io.kagera.api.multiset._

object Marking {

  type MarkingData = Map[Place[_], MultiSet[_]]

  def empty: Marking = Marking(Map.empty[Place[_], MultiSet[_]])

  def apply[A](m1: MarkedPlace[A]): Marking = {
    Marking(Map(m1): Map[Place[_], MultiSet[_]])
  }

  def apply[A, B](m1: MarkedPlace[A], m2: MarkedPlace[B]): Marking = {
    Marking(Map(m1, m2): Map[Place[_], MultiSet[_]])
  }

  def apply[A, B, C](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C]): Marking = {
    Marking(Map(m1, m2, m3): Map[Place[_], MultiSet[_]])
  }

  def apply[A, B, C, D](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C], m4: MarkedPlace[D]): Marking = {
    Marking(Map(m1, m2, m3, m4): Map[Place[_], MultiSet[_]])
  }
}

// TODO generalize this, shapeless HMap seems a good fit, however we need to know the keys
case class Marking(data: MarkingData) {

  def get[C](p: Place[C]): Option[MultiSet[C]] = data.get(p).map(_.asInstanceOf[MultiSet[C]])

  def getOrElse[C](p: Place[C], mset: MultiSet[C]): MultiSet[C] = get(p).getOrElse(mset)

  def getOrEmpty[C](p: Place[C]): MultiSet[C] = getOrElse(p, MultiSet.empty[C])

  def apply[C](p: Place[C]): MultiSet[C] = get(p).get

  def getTokens[C](p: Place[C]): Iterable[C] = apply(p).allElements

  def contains(p: Place[_]) = data.contains(p)

  def multiplicities: MultiSet[Place[_]] = data.mapValues(_.multisetSize)

  def markedPlaces: Set[Place[_]] = data.keySet

  def add[C](place: Place[C], token: C, count: Int) =
    data + (place -> getOrEmpty(place).multisetIncrement(token, count))

  def add[C](place: Place[C], token: C): Marking = add(place, token, 1)

  def +[C](tuple: (Place[C], MultiSet[C])): Marking = tuple match {
    case (place, tokens) => data + (place -> tokens)
  }

  def -[C](place: Place[C]): Marking = data - place

  def --(other: Marking): Marking = other.markedPlaces.foldLeft(data) { case (result, place) =>
    get(place) match {
      case None => result
      case Some(tokens) =>
        val newTokens: MultiSet[_] = tokens.multisetDifference(other(place))
        if (newTokens.isEmpty)
          result - place
        else
          result + (place -> newTokens)
    }
  }

  def ++(other: Marking): Marking = other.markedPlaces.foldLeft(data) { case (result, place) =>
    val newTokens = get(place) match {
      case None => other(place)
      case Some(tokens) => tokens.multisetSum(other(place))
    }

    result + (place -> newTokens)
  }

  override def toString = data.mapValues(_.allElements.mkString("(", ",", ")")).mkString("Marking(", ",", ")")
}