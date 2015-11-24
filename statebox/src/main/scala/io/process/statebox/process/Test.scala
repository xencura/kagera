package io.process.statebox.process

import akka.actor.Actor
import io.process.statebox.process.Test._

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.WDiEdge
import scalax.collection.GraphPredef.Param
import scalax.collection.immutable.Graph

object Test {

  object Place {
    def apply[A](l: String) = new Place[A] {
      override val label = l
    }
  }

  sealed trait Place[Color] {
    def id: Long = hashCode()
    def label: String
  }

  sealed trait Transition[I, O] extends Function[I, O] {
    def id: Long = hashCode()
    def label: String
  }

  implicit def toTransition[I, O](fn: I => O): Transition[I, O] = new Transition[I, O] {
    override def label: String = ???
    override def apply(v1: I): O = ???
  }

  implicit def toTransition[O](fn: () => O): Transition[Unit, O] = new Transition[Unit, O] {
    override def label: String = ???
    override def apply(v1: Unit): O = ???
  }

  type Node = Either[Place[_], Transition[_, _]]
  type Arc = WDiEdge[Node]
  type PTNet = Graph[Node, WDiEdge]

  def arc(t: Transition[_, _], p: Place[_], weight: Long): Arc = WDiEdge[Node](Right(t), Left(p))(weight)
  def arc(p: Place[_], t: Transition[_, _], weight: Long): Arc = WDiEdge[Node](Left(p), Right(t))(weight)

  type |~>[A, B] = Transition[A, B]

  type Token[T] = (Place[T], T)

  trait Marking[T] {
    def marking: Map[Place[_], Long]
  }

  implicit def %[A](p: Place[A]): Marking[A] = new Marking[A] {
    override val marking = Map[Place[_], Long](p -> 1)
  }

  implicit def %[A, B](places: (Place[A], Place[B])): Marking[(A, B)] = new Marking[(A, B)] {
    override val marking = Map[Place[_], Long](places._1 -> 1, places._2 -> 1)
  }

  implicit class TF[A, B](t: Transition[A, B]) {
    def ~>(m: Marking[B]) = m.marking.map { case (p, weight) => arc(t, p, weight) }.toSeq
  }

  implicit class M[A](m: Marking[A]) {
    def ~>[B](t: Transition[A, B]) = m.marking.map { case (p, weight) => arc(p, t, weight) }.toSeq
  }

  def process(params: Seq[Arc]*): PTNet = Graph(params.reduce(_ ++ _): _*)

  case object Step
}

class Test extends Actor {

  import Test._

  val a = Place[Int]("a")
  val b = Place[Int]("b")

  val f = process(toTransition(init) ~> %(a, b), %(a, b) ~> sum)

  val init = () => (5, 5)
  def sum: (Int, Int) |~> Int = ???

  def add(a: Int, b: Int) = a + b

  var state: Set[Token[_]] = Set.empty

  //  def marking: Map[Place[_], Long]

  //  def outAdjacentTransitions: Iterable[Transition[_,_]]
  //
  //  def enabledTransitions(): Iterable[Transition[_,_]] = {
  //
  //    marking.keys.map { p: Place[_] =>
  //
  //      p.id
  //    }
  //  }

  override def receive: Receive = { case _ =>

  // find enabled transitions

  }
}
