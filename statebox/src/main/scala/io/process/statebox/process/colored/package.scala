package io.process.statebox.process

import io.process.statebox.process.ScalaGraph._
import io.process.statebox.process.simple.{ SimpleExecutor, SimpleTokenGame }

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object colored {

  object Place {
    def apply[A](id: Long, label: String) = PlaceImpl[A](id, label)
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

  case class PlaceImpl[C](override val id: Long, override val label: String) extends Place {
    type Color = C
  }

  case class TransitionImpl[I, O](override val id: Long, override val label: String) extends Transition {
    type Input = I
    type Output = O
  }

  case class TransitionFn(id: Long, label: String) {
    def apply[O](fn: () => O) = new TransitionImpl[Unit, O](id, label)
    def apply[A, O](fn: (A) => O) = new TransitionImpl[A, O](id, label)
    def apply[A, B, O](fn: (A, B) => O) = new TransitionImpl[(A, B), O](id, label)
    def apply[A, B, C, O](fn: (A, B, C) => O) = new TransitionImpl[(A, B, C), O](id, label)
  }

  type Node = Either[Place, Transition]
  type Arc = WDiEdge[Node]

  def arc(t: Transition, p: Place, weight: Long): Arc = WDiEdge[Node](Right(t), Left(p))(weight)
  def arc(p: Place, t: Transition, weight: Long): Arc = WDiEdge[Node](Left(p), Right(t))(weight)

  sealed trait MarkingSpec[T] {
    def marking: Map[Place, Long]
  }

  implicit def %[A](p: Place { type Color = A }): MarkingSpec[A] = new MarkingSpec[A] {
    override val marking = Map[Place, Long](p -> 1)
  }

  implicit def %[A, B](places: (Place { type Color = A }, Place { type Color = B })): MarkingSpec[(A, B)] =
    new MarkingSpec[(A, B)] {
      override val marking = Map[Place, Long](places._1 -> 1, places._2 -> 1)
    }

  implicit class TF[B](t: Transition { type Output = B }) {
    def ~>(m: MarkingSpec[B]) = m.marking.map { case (p, weight) => arc(t, p, weight) }.toSeq
  }

  implicit class M[A](m: MarkingSpec[A]) {
    def ~>[B](t: Transition { type Input = A }) = m.marking.map { case (p, weight) => arc(p, t, weight) }.toSeq
  }

  type Token[D] = (Long, D)

  type ColouredMarking[P] = Map[P, Set[Token[_]]]

  def coulouredMarkingLike[P]: MarkingLike[ColouredMarking[P], P] = new MarkingLike[ColouredMarking[P], P] {
    override def emptyMarking: ColouredMarking[P] = Map.empty

    override def tokenCount(marking: ColouredMarking[P]): Marking[P] = marking.map { case (p, tokens) =>
      (p, tokens.size.toLong)
    }.toMap

    override def consume(from: ColouredMarking[P], other: ColouredMarking[P]): ColouredMarking[P] = ???

    override def produce(into: ColouredMarking[P], other: ColouredMarking[P]): ColouredMarking[P] = ???

    override def isSubMarking(m: ColouredMarking[P], other: ColouredMarking[P]): Boolean = ???
  }

  def process(params: Seq[Arc]*): PTProcess[Place, Transition, Marking[Place]] =
    new ScalaGraphWrapper(Graph(params.reduce(_ ++ _): _*))
      with SimpleExecutor[Place, Transition]
      with SimpleTokenGame[Place, Transition]
}
