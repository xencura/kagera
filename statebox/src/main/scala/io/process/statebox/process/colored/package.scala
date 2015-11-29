package io.process.statebox.process

import io.process.statebox.process.ScalaGraph._

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object colored {

  object Place {
    def apply[A](l: String) = new Place {
      type Color = A
      override val label = l
    }
  }

  trait Place extends HasLabel with Identifiable {
    type Color
    def id: Long = label.hashCode
    override def toString = label
  }

  trait Transition extends HasLabel with Identifiable {
    type Input
    type Output
    override def toString = label
    def id: Long = label.hashCode
  }

  def toTransition2[A, B, OUT](name: String, fn: (A, B) => OUT) = new Transition {
    type Input = (A, B)
    type Output = OUT
    override def label: String = name
  }

  def toTransition0[OUT](name: String, fn: () => OUT) = new Transition {
    type Input = Unit
    type Output = OUT
    override def label: String = name
  }

  type Node = Either[Place, Transition]
  type Arc = WDiEdge[Node]
  type ColoredPetriNet = BiPartiteGraph[Place, Transition]

  def arc(t: Transition, p: Place, weight: Long): Arc = WDiEdge[Node](Right(t), Left(p))(weight)
  def arc(p: Place, t: Transition, weight: Long): Arc = WDiEdge[Node](Left(p), Right(t))(weight)

  type Token[T] = (Place { type Color = T }, T)

  sealed trait MarkingHolder[T] {
    def marking: Map[Place, Long]
  }

  implicit def %[A](p: Place { type Color = A }): MarkingHolder[A] = new MarkingHolder[A] {
    override val marking = Map[Place, Long](p -> 1)
  }

  implicit def %[A, B](places: (Place { type Color = A }, Place { type Color = B })): MarkingHolder[(A, B)] =
    new MarkingHolder[(A, B)] {
      override val marking = Map[Place, Long](places._1 -> 1, places._2 -> 1)
    }

  implicit class TF[B](t: Transition { type Output = B }) {
    def ~>(m: MarkingHolder[B]) = m.marking.map { case (p, weight) => arc(t, p, weight) }.toSeq
  }

  implicit class M[A](m: MarkingHolder[A]) {
    def ~>[B](t: Transition { type Input = A }) = m.marking.map { case (p, weight) => arc(p, t, weight) }.toSeq
  }

  def process(params: Seq[Arc]*): PetriNet[Place, Transition] = new ScalaGraphWrapper(Graph(params.reduce(_ ++ _): _*))
}
