package io.kagera.api

import io.kagera.api.multiset.MultiSet

class Experiment extends App {

  //  import shapeless.poly._
  //
  //  case class RMap[K[_], V[_]](backingMap: Map[K[_], V[_]]) {
  //
  //    def -[C](key: K[C]) = RMap(backingMap - key)
  //
  //    def get[C](key: K[C]): Option[V[C]] = backingMap.get(key).asInstanceOf[Option[V[C]]]
  //
  //    def apply[C](key: K[C]): V[C] = get(key).get
  //
  //    def keySet: Set[K[_]] = backingMap.keySet
  //
  //    def mapValues[T[_]](fn: V ~> T) = ???
  //  }
  //
  //  type Process[S, A]
  //
  //  type Action[S] = S => S
  //
  //  type Topology[S, Action[S]] = S => Set[Action[S]]
  //
  //  type Router[S] = Set[Action[S]] => Action[S]

  //  implicit def placeMapping[C] = new Mapping[Place[C], MultiSet[C]]
  //
  //  type Mark = HMap[Mapping]
  //
  //  val p: Place[Unit] = new PlaceImpl[Unit](id = 1, label = "d")
  //
  //  val map = HMap[Mapping](p -> MultiSet(()))
  //
  //  object fn extends (MultiSet ~> Option) {
  //    override def apply[T](f: MultiSet[T]): Option[T] = f.keys.headOption
  //  }
}
