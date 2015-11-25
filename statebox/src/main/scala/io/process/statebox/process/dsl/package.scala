package io.process.statebox.process

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object dsl {

  object Place {
    def apply[A](l: String) = new Place {
      type Color = A
      override val label = l
    }
  }

  sealed trait Place {
    type Color
    override def toString = label
    def id: Long = label.hashCode
    def label: String
  }

  sealed trait Transition[I, O] {
    def id: Long = label.hashCode
    override def toString = label
    def label: String
  }

  implicit def toTransition2[A, B, O](name: String, fn: (A, B) => O): Transition[(A, B), O] = new Transition[(A, B), O] {
    override def label: String = name
  }

  implicit def toTransition0[O](name: String, fn: () => O): Transition[Unit, O] = new Transition[Unit, O] {
    override def label: String = name
  }

  type Node = Either[Place, Transition[_, _]]
  type Arc = WDiEdge[Node]
  type PetriNet = Graph[Node, WDiEdge]

  def arc(t: Transition[_, _], p: Place, weight: Long): Arc = WDiEdge[Node](Right(t), Left(p))(weight)
  def arc(p: Place, t: Transition[_, _], weight: Long): Arc = WDiEdge[Node](Left(p), Right(t))(weight)

  type Token[T] = (Place { type Color = T }, T)

  trait Marking[T] {
    def marking: Map[Place, Long]
  }

  implicit def %[A](p: Place { type Color = A }): Marking[A] = new Marking[A] {
    override val marking = Map[Place, Long](p -> 1)
  }

  implicit def %[A, B](places: (Place { type Color = A }, Place { type Color = B })): Marking[(A, B)] =
    new Marking[(A, B)] {
      override val marking = Map[Place, Long](places._1 -> 1, places._2 -> 1)
    }

  implicit class TF[A, B](t: Transition[A, B]) {
    def ~>(m: Marking[B]) = m.marking.map { case (p, weight) => arc(t, p, weight) }.toSeq
  }

  implicit class M[A](m: Marking[A]) {
    def ~>[B](t: Transition[A, B]) = m.marking.map { case (p, weight) => arc(p, t, weight) }.toSeq
  }

  def process(params: Seq[Arc]*): PetriNet = Graph(params.reduce(_ ++ _): _*)

  implicit def toNode(p: Place): Node = Left(p)
  implicit def toNode(t: Transition[_, _]): Node = Right(t)
  implicit class NodeAdditions(node: Node) {
    def asPlace(): Place = node match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }
    def asTransition(): Transition[_, _] = node match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }
  }

  implicit class PetriNetAdditions(process: PetriNet) {

    def enabledTransitions(marking: Map[Place, Long]) = {
      marking
        .map { case (p, cnt) =>
          process.get(p).outgoing.collect {
            case edge if (edge.weight <= cnt) => edge.target.value.asTransition()
          }
        }
        .reduce(_ ++ _)
    }

    def places() = process.nodes.map(_.value).collect { case Left(place) => place }

    def transitions() = process.nodes.map(_.value).collect { case Right(transition) => transition }

    def toDot() = {

      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._

      val root = DotRootGraph(
        directed = true,
        id = Some("Process"),
        attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
        attrList = List(DotAttr("attr_1", """"one""""), DotAttr("attr_2", "<two>"))
      )

      def nodeId(node: PetriNet#NodeT): String = {
        node.value match {
          case Left(place) => place.label
          case Right(transition) => transition.label
        }
      }

      def myNodeTransformer(innerNode: PetriNet#NodeT): Option[(DotGraph, DotNodeStmt)] = innerNode.value match {
        case Left(place) => Some((root, DotNodeStmt(place.label, List(DotAttr("shape", "circle")))))
        case Right(transition) => Some((root, DotNodeStmt(transition.label, List(DotAttr("shape", "square")))))
      }

      def myEdgeTransformer(innerEdge: PetriNet#EdgeT): Option[(DotGraph, DotEdgeStmt)] = innerEdge.edge match {
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
