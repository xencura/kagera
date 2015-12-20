package io.process.statebox.process

import io.process.statebox.process.ScalaGraph._
import io.process.statebox.process.simple.{ SimpleExecutor, SimpleTokenGame }

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object colored {

  object Place {
    def apply[A](l: String) = new Place {
      type Color = A
      override val label = l
    }
  }

  trait Place {
    type Color
    def id: Long = label.hashCode
    def label: String
    override def toString = label
  }

  trait Transition {
    type Input
    type Output
    def label: String
    override def toString = label
    def id: Long = label.hashCode
  }

  case class TFn(id: Long, l: String) {
    def apply[O](fn: () => O) = new Transition {
      type Input = Unit
      type Output = O
      override def label = l
    }
    def apply[A, B, O](fn: (A, B) => O) = new Transition {
      type Input = (A, B)
      type Output = O
      override def label = l
    }
  }

  type Node = Either[Place, Transition]
  type Arc = WDiEdge[Node]

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

  def process(params: Seq[Arc]*): PTProcess[Place, Transition, Marking[Place]] =
    new ScalaGraphWrapper(Graph(params.reduce(_ ++ _): _*))
      with SimpleExecutor[Place, Transition]
      with SimpleTokenGame[Place, Transition]

}
