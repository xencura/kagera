package io.kagera.api.colored

import io.kagera.api.multiset._

object ColoredMarking {

  def empty: ColoredMarking = ColoredMarking(Nil: _*)

  def apply(markedPlaces: MarkedPlace[_]*): ColoredMarking = {
    val map: Map[Place[_], MultiSet[_]] = Map(markedPlaces.map(mp => (mp.place, mp.tokens)): _*)
    ColoredMarking(map)
  }
}

case class ColoredMarking(data: Map[Place[_], MultiSet[_]]) {

  implicit def toMarking(map: Map[Place[_], MultiSet[_]]): ColoredMarking = ColoredMarking(map)

  def get[C](p: Place[C]): Option[MultiSet[C]] = data.get(p).map(_.asInstanceOf[MultiSet[C]])

  def apply[C](p: Place[C]): MultiSet[C] = get(p).get

  def getTokens[C](p: Place[C]): Iterable[C] = apply(p).allElements

  def contains(p: Place[_]) = data.contains(p)

  def multiplicities: Map[Place[_], Int] = data.mapValues(_.multisetSize)

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
