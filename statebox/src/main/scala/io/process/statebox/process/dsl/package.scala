package io.process.statebox.process

import scala.PartialFunction._
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object dsl {

  trait Identifiable {
    def id: Long
  }

  trait HasLabel {
    def label: String
  }

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
  type ColoredPetriNet = Graph[Node, WDiEdge]

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

  def process(params: Seq[Arc]*): ColoredPetriNet = Graph(params.reduce(_ ++ _): _*)

  // ----- READING

  implicit def toNode(p: Place): Node = Left(p)
  implicit def toNode(t: Transition): Node = Right(t)

  implicit class NodeAdditions(node: Node) {

    import PartialFunction._

    def isPlace = cond(node) { case Left(place) => true }
    def isTransition = cond(node) { case Right(transition) => true }
  }

  implicit class NodeTAdditions(node: ColoredPetriNet#NodeT) {
    def asPlace: Place = node.value match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }
    def asTransition: Transition = node.value match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }

    def isPlace = cond(node.value) { case Left(place) => true }
    def isTransition = cond(node.value) { case Right(transition) => true }
  }

  implicit class PetriNetAdditions(val process: ColoredPetriNet) {

    def enabledTransitions(marking: Map[Place, Long]): Set[Transition] = {
      marking
        .map { case (place, count) =>
          process.get(place).outgoing.collect {
            case edge if (edge.weight <= count) => edge.target
          }
        }
        .reduceOption(_ ++ _)
        .getOrElse(Set.empty)
        .collect {
          case node if incomingPlaces(node).subsetOf(marking.keySet) => node.asTransition
        } ++ constructors
    }

    def incomingPlaces(node: ColoredPetriNet#NodeT): Set[Place] = node.incoming.map(_.source.asPlace)
    def incomingPlaces(t: Transition): Set[Place] = incomingPlaces(process.get(t))

    def outgoingPlaces(node: ColoredPetriNet#NodeT): Set[Place] = node.outgoing.map(_.target.asPlace)
    def outgoingPlaces(t: Transition): Set[Place] = outgoingPlaces(process.get(t))

    def incomingTransitions(p: Place): Set[Transition] = incomingTransitions(process.get(p))
    def incomingTransitions(node: ColoredPetriNet#NodeT): Set[Transition] = node.incoming.map(_.source.asTransition)

    def outgoingTransitions(node: ColoredPetriNet#NodeT): Set[Transition] = node.outgoing.map(_.target.asTransition)
    def outgoingTransitions(t: Place): Set[Transition] = outgoingTransitions(process.get(t))

    def inMarking(t: Transition): Map[Place, Long] = process.get(t).incoming.map(e => e.source.asPlace -> e.weight).toMap
    def outMarking(t: Transition): Map[Place, Long] =
      process.get(t).outgoing.map(e => e.target.asPlace -> e.weight).toMap

    lazy val constructors = process.nodes.collect {
      case node if node.isTransition && node.incoming.isEmpty => node.asTransition
    }

    def places() = process.nodes.collect { case n if n.isPlace => n.asPlace }

    def transitions() = process.nodes.collect { case n if n.isTransition => n.asTransition }

    def toDot() = {

      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._

      val root = DotRootGraph(
        directed = true,
        id = Some("Process"),
        attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
        attrList = List(DotAttr("attr_1", """"one""""), DotAttr("attr_2", "<two>"))
      )

      def nodeId(node: ColoredPetriNet#NodeT): String = {
        node.value match {
          case Left(place) => place.label
          case Right(transition) => transition.label
        }
      }

      def myNodeTransformer(innerNode: ColoredPetriNet#NodeT): Option[(DotGraph, DotNodeStmt)] = innerNode.value match {
        case Left(place) => Some((root, DotNodeStmt(place.label, List(DotAttr("shape", "circle")))))
        case Right(transition) => Some((root, DotNodeStmt(transition.label, List(DotAttr("shape", "square")))))
      }

      def myEdgeTransformer(innerEdge: ColoredPetriNet#EdgeT): Option[(DotGraph, DotEdgeStmt)] = innerEdge.edge match {
        case WDiEdge(source, target, weight) =>
          Some((root, DotEdgeStmt(nodeId(source), nodeId(target), List(DotAttr("weight", weight.toString)))))
      }

      graph2DotExport(process).toDot(
        dotRoot = root,
        edgeTransformer = myEdgeTransformer,
        cNodeTransformer = Some(myNodeTransformer)
      )
    }
  }
}
