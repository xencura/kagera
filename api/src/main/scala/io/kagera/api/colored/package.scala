package io.kagera.api

import io.kagera.api.ScalaGraph._
import io.kagera.api.simple.{ SimpleExecutor, SimpleTokenGame }
import io.kagera.api.tags.Label
import shapeless.{ HList, HNil }
import shapeless.ops.hlist._

import scala.concurrent.Future
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge
import scalaz.{ @@, Tag }

package object colored {

  type Node = Either[Place, Transition]

  type Arc = WLDiEdge[Node]

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

  def arc(t: Transition, p: Place, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, fieldName)

  def arc(p: Place, t: Transition, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Left(p), Right(t))(weight, fieldName)

  sealed trait MarkingSpec[T] {
    def marking: Seq[(Place, Long)]
  }

  implicit def %[A](p: Place { type Color = A }): MarkingSpec[A] = new MarkingSpec[A] {
    override val marking = Seq(p -> 1L)
  }

  implicit def %[A, B](places: (Place { type Color = A }, Place { type Color = B })): MarkingSpec[(A, B)] =
    new MarkingSpec[(A, B)] {
      override val marking = Seq(places._1 -> 1L, places._2 -> 1L)
    }

  implicit class TF[B](t: Transition { type Output = B }) {
    def ~>(m: MarkingSpec[B]) = m.marking.zipWithIndex.map { case ((p, weight), index) => arc(t, p, weight, index) }
  }

  implicit class M[A](m: MarkingSpec[A]) {
    def ~>[B](t: Transition { type Input = A }) = m.marking.zipWithIndex.map { case ((p, weight), index) =>
      arc(p, t, weight, index)
    }
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

    def createTransitionInput(marking: ColouredMarking, t: Transition) = {
      //      if (marking.isEmpty) {
      //        ()
      //      } else {
      //
      //        // get in-adjacent arcs
      //        // need to know the place order for the function
      //        val mapped = innerGraph.get(Right(t)).incoming.map { edge =>
      //          edge.source.valueA -> edge.label.asInstanceOf[Int]
      //        }.toSeq.sortBy { case (place, index) => index }
      //
      //        mapped.foldLeft[HList](HNil) {
      //          case (hlist, (place, index)) => marking.get(place) match {
      //            case None         => throw new IllegalStateException("")
      //            case Some(tokens) => tokens.head._2 :: hlist
      //          }
      //        }.tupled
      //      }
    }

    override def fireTransition(m: ColouredMarking)(t: Transition): ColouredMarking = {

      // pick the tokens
      enabledParameters(m)(t).headOption.foreach { marking =>
        val input = createTransitionInput(m, t)
        val out = t.apply(input.asInstanceOf[t.Input])

      // create transition input from tokens in places
      // execute the transition
      }

      ???
    }
  }

  def process(params: Seq[Arc]*): PTProcess[Place, Transition, Marking[Place]] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*))
      with SimpleTokenGame[Place, Transition]
      with SimpleExecutor[Place, Transition]
}
