package io.kagera

import scala.PartialFunction._
import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.edge.WLDiEdge
import scalaz.@@
import scala.language.higherKinds

package object api {

  type Marking[P] = Map[P, Long]

  object tags {
    trait Id
    trait Weight
    trait Label
  }

  // TODO decide, shapeless or scalaz tags?
  type Identifiable[T] = T => Long @@ tags.Id
  type Labeled[T] = T => String @@ tags.Label

  implicit class LabeledFn[T : Labeled](seq: Iterable[T]) {
    def findByLabel(label: String) = seq.find(e => implicitly[Labeled[T]].apply(e) == label)
  }

  implicit class IdFn[T : Identifiable](seq: Iterable[T]) {
    def findById(id: String) = seq.find(e => implicitly[Identifiable[T]].apply(e) == id)
  }

  /**
   * Type alias for a petri net with token game and executor. This makes an executable process.
   *
   * @tparam P
   *   The place type
   * @tparam T
   *   The transition type
   * @tparam M
   *   The marking type
   */
  trait PetriNetProcess[P, T, M] extends PetriNet[P, T] with TokenGame[P, T, M] with TransitionExecutor[P, T, M]

  implicit class MarkingLikeApi[M, P](val m: M)(implicit val markingLike: MarkingLike[M, P]) {

    def multiplicity = markingLike.multiplicity(m)

    def consume(other: M) = markingLike.consume(m, other)

    def remove(other: M) = markingLike.remove(m, other)

    def produce(other: M) = markingLike.produce(m, other)

    def isEmpty = markingLike.multiplicity(m).isEmpty

    def isSubMarking(other: M) = markingLike.isSubMarking(m, other)
  }

  trait TransitionExecutor[P, T, M] {

    this: PetriNet[P, T] =>

    def fireTransition(marking: M, id: java.util.UUID)(transition: T, data: Option[Any] = None)(implicit
      ec: ExecutionContext
    ): Future[M]
  }

  type BiPartiteGraph[P, T, E[X] <: EdgeLikeIn[X]] = Graph[Either[P, T], E]

  class ScalaGraphPetriNet[P, T](val innerGraph: BiPartiteGraph[P, T, WLDiEdge]) extends PetriNet[P, T] {

    override def inMarking(t: T): Marking[P] = innerGraph.get(t).incoming.map(e => e.source.asPlace -> e.weight).toMap
    override def outMarking(t: T): Marking[P] = innerGraph.get(t).outgoing.map(e => e.target.asPlace -> e.weight).toMap
    override def outAdjacentPlaces(t: T): Set[P] = innerGraph.outgoingPlaces(t)
    override def outAdjacentTransitions(p: P): Set[T] = innerGraph.outgoingTransitions(p)
    override def inAdjacentPlaces(t: T): Set[P] = innerGraph.incomingPlaces(t)
    override def inAdjacentTransitions(p: P): Set[T] = innerGraph.incomingTransitions(p)

    override lazy val places = innerGraph.places().toSet
    override lazy val transitions = innerGraph.transitions().toSet

    override def nodes = innerGraph.nodes.map(_.value)
  }

  implicit def placeToNode[P, T](p: P): Either[P, T] = Left(p)
  implicit def transitionToNode[P, T](t: T): Either[P, T] = Right(t)

  implicit class DirectedBiPartiteNodeTAdditions[A, B](val node: BiPartiteGraph[A, B, WLDiEdge]#NodeT) {

    def asPlace: A = node.value match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }

    def asTransition: B = node.value match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a transition!")
    }

    def incomingEdgeB(b: B) = node.incoming.find(_.source.value == Right(b)).map(_.toOuter)
    def outgoingEdgeB(b: B) = node.outgoing.find(_.target.value == Right(b)).map(_.toOuter)

    def incomingEdgeA(a: A) = node.incoming.find(_.source.value == Left(a)).map(_.toOuter)
    def outgoingEdgeA(a: A) = node.outgoing.find(_.target.value == Left(a)).map(_.toOuter)

    def incomingPlaces = node.incoming.map(_.source.asPlace)
    def incomingTransitions = node.incoming.map(_.source.asTransition)

    def outgoingPlaces = node.outgoing.map(_.target.asPlace)
    def outgoingTransitions = node.outgoing.map(_.target.asTransition)

    def isPlace = cond(node.value) { case Left(n) => true }
    def isTransition = cond(node.value) { case Right(n) => true }
  }

  implicit class DirectedBiPartiteGraphAdditions[P, T](val graph: BiPartiteGraph[P, T, WLDiEdge]) {

    def findPTEdge(from: P, to: T): Option[WLDiEdge[Either[P, T]]] =
      graph.get(Left(from)).outgoing.find(_.target.value == Right(to)).map(_.toOuter)

    def findTPEdge(from: T, to: P): Option[WLDiEdge[Either[P, T]]] =
      graph.get(Right(from)).outgoing.find(_.target.value == Left(to)).map(_.toOuter)

    def incomingPlaces(t: T): Set[P] = graph.get(t).incomingPlaces

    def incomingTransitions(p: P): Set[T] = graph.get(p).incomingTransitions

    def outgoingPlaces(t: T): Set[P] = graph.get(t).outgoingPlaces

    def outgoingTransitions(p: P): Set[T] = graph.get(p).outgoingTransitions

    def places() = graph.nodes.collect { case n if n.isPlace => n.asPlace }

    def transitions() = graph.nodes.collect { case n if n.isTransition => n.asTransition }
  }
}
