package io.kagera.api

import io.kagera.api.ScalaGraph._
import io.kagera.api.simple.{ SimpleExecutor, SimpleTokenGame }
import io.kagera.api.tags.Label

import scala.concurrent.Future
import scalax.collection.Graph
import scalax.collection.GraphEdge.{ DiEdgeLike, EdgeCopy }
import scalax.collection.GraphPredef.OuterEdge
import scalax.collection.edge.WBase.WEdgeCompanion
import scalax.collection.edge.{ WDiEdge, WUnDiEdge }
import scalaz.{ @@, Tag }

package object colored {

  type Node = Either[Place, Transition]

  type Token[D] = (Long, D)

  type ColouredMarking = Map[Place, Set[Token[_]]]

  implicit object PlaceLabeler extends Labeled[Place] {
    override def apply(p: Place): @@[String, Label] = Tag[String, Label](p.label)
  }

  implicit object TransitionLabeler extends Labeled[Transition] {
    override def apply(t: Transition): @@[String, Label] = Tag[String, Label](t.label)
  }

  case class PlaceImpl[C](override val id: Long, override val label: String) extends Place {
    type Color = C
  }

  case class TransitionImpl[I, O](override val id: Long, override val label: String, fn: I => O) extends Transition {
    type Input = I
    type Output = O

    override def apply(input: Input): Future[Output] = Future.successful(fn(input))
  }

  case class TransitionFn(id: Long, label: String) {
    def apply[O](fn: () => O) = new TransitionImpl[Unit, O](id, label, unit => fn())
    def apply[A, O](fn: A => O) = new TransitionImpl[A, O](id, label, fn)
    def apply[A, B, O](fn: (A, B) => O) = new TransitionImpl[(A, B), O](id, label, fn.tupled)
    def apply[A, B, C, O](fn: (A, B, C) => O) = new TransitionImpl[(A, B, C), O](id, label, fn.tupled)
  }

  def arc(t: Transition, p: Place, weight: Long): WDiEdge[Node] = WDiEdge[Node](Right(t), Left(p))(weight)
  def arc(p: Place, t: Transition, weight: Long): WDiEdge[Node] = WDiEdge[Node](Left(p), Right(t))(weight)

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

  implicit object CoulouredMarkingLike extends MarkingLike[ColouredMarking, Place] {

    override def emptyMarking: ColouredMarking = Map.empty
    override def multiplicity(marking: ColouredMarking): Marking[Place] = marking.map { case (p, tokens) =>
      (p, tokens.size.toLong)
    }.toMap
    override def consume(from: ColouredMarking, other: ColouredMarking): ColouredMarking = ???
    override def produce(into: ColouredMarking, other: ColouredMarking): ColouredMarking = ???
    override def isSubMarking(m: ColouredMarking, other: ColouredMarking): Boolean = ???
  }

  trait ColouredExecutor extends TransitionExecutor[Place, Transition, ColouredMarking] {

    this: PetriNet[Place, Transition] with TokenGame[Place, Transition, ColouredMarking] =>

    override def fireTransition(m: ColouredMarking)(t: Transition): ColouredMarking = {

      // pick the tokens
      enabledParameters(m)(t).headOption.foreach { marking =>
        // get in-adjacent arcs
        // need to know the place order for the function

        innerGraph.get(Right(t)).incoming.map { edge =>
          val v = edge.source

          ???
        }

        // create transition input from input

        // execute the transition
      }

      ???
    }
  }

  def process(params: Seq[WDiEdge[Node]]*): PTProcess[Place, Transition, Marking[Place]] =
    new ScalaGraphWrapper(Graph(params.reduce(_ ++ _): _*))
      with SimpleTokenGame[Place, Transition]
      with SimpleExecutor[Place, Transition]
}
