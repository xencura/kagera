package io.kagera.api.colored

import io.kagera.api.colored.ColoredMarking.MarkingData
import io.kagera.api.multiset._

object ColoredMarking {

  type MarkingData = Map[Place[_], MultiSet[_]]

  def empty: ColoredMarking = ColoredMarking(Map.empty[Place[_], MultiSet[_]])

  def apply[A](m1: MarkedPlace[A]): ColoredMarking = {
    ColoredMarking(Map(m1): Map[Place[_], MultiSet[_]])
  }

  def apply[A, B](m1: MarkedPlace[A], m2: MarkedPlace[B]): ColoredMarking = {
    ColoredMarking(Map(m1, m2): Map[Place[_], MultiSet[_]])
  }

  def apply[A, B, C](m1: MarkedPlace[A], m2: MarkedPlace[B], m3: MarkedPlace[C]): ColoredMarking = {
    ColoredMarking(Map(m1, m2, m3): Map[Place[_], MultiSet[_]])
  }
}

case class ColoredMarking(data: MarkingData) {

  def get[C](p: Place[C]): Option[MultiSet[C]] = data.get(p).map(_.asInstanceOf[MultiSet[C]])

  def apply[C](p: Place[C]): MultiSet[C] = get(p).get

  def getTokens[C](p: Place[C]): Iterable[C] = apply(p).allElements

  def contains(p: Place[_]) = data.contains(p)

  def multiplicities: MultiSet[Place[_]] = data.mapValues(_.multisetSize)

  def markedPlaces: Set[Place[_]] = data.keySet

  def +[C](tuple: (Place[C], MultiSet[C])): ColoredMarking = tuple match {
    case (place, tokens) => data + (place -> tokens)
  }

  def -[C](place: Place[C]): ColoredMarking = data - place

  def --(other: ColoredMarking): ColoredMarking = other.markedPlaces.foldLeft(data) { case (result, place) =>
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

  def ++(other: ColoredMarking): ColoredMarking = other.markedPlaces.foldLeft(data) { case (result, place) =>
    val newTokens = get(place) match {
      case None => other(place)
      case Some(tokens) => tokens.multisetSum(other(place))
    }

    result + (place -> newTokens)
  }

  override def toString = data.mapValues(_.allElements.mkString("(", ",", ")")).toString
}
