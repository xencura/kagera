package io.kagera.api

import scala.PartialFunction._
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot.DotAttr

object ScalaGraph {

  type BiPartiteGraph[P, T] = Graph[Either[P, T], WDiEdge]

  class ScalaGraphWrapper[P, T](val innerGraph: BiPartiteGraph[P, T]) extends PetriNet[P, T] {

    override def inMarking(t: T): Marking[P] = innerGraph.get(t).incoming.map(e => e.source.valueA -> e.weight).toMap
    override def outMarking(t: T): Marking[P] = innerGraph.get(t).outgoing.map(e => e.target.valueA -> e.weight).toMap

    override lazy val places = innerGraph.nodesA().toSet
    override lazy val transitions = innerGraph.nodesB().toSet

    override def nodes() = innerGraph.nodes.map(_.value)
  }

  implicit def placeToNode[P, T](p: P): Either[P, T] = Left(p)
  implicit def transitionToNode[P, T](t: T): Either[P, T] = Right(t)

  implicit class BiPartiteNodeTAdditions[P, T](val node: BiPartiteGraph[P, T]#NodeT) {

    def valueA: P = node.value match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }

    def valueB: T = node.value match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a transition!")
    }

    def incomingA = node.incoming.map(_.source.valueA)
    def incomingB = node.incoming.map(_.source.valueB)

    def outgoingA = node.outgoing.map(_.target.valueA)
    def outgoingB = node.outgoing.map(_.target.valueB)

    def isNodeA = cond(node.value) { case Left(n) => true }
    def isNodeB = cond(node.value) { case Right(n) => true }
  }

  implicit class BiPartiteGraphAdditions[P, T](val process: BiPartiteGraph[P, T]) {

    import scalax.collection.io.dot._
    import scalax.collection.io.dot.implicits._

    def incomingA(t: T): Set[P] = process.get(t).incomingA
    def outgoingA(t: T): Set[P] = process.get(t).outgoingA
    def incomingB(p: P): Set[T] = process.get(p).incomingB
    def outgoingB(p: P): Set[T] = process.get(p).outgoingB

    def nodesA() = process.nodes.collect { case n if n.isNodeA => n.valueA }
    def nodesB() = process.nodes.collect { case n if n.isNodeB => n.valueB }

    type ShapeFn = Either[P, T] => scala.Seq[scalax.collection.io.dot.DotAttr]
    type LabelFn = Either[P, T] => String

    val defaultShapFn: ShapeFn = node =>
      node match {
        case Left(nodeA) => List(DotAttr("shape", "circle"))
        case Right(nodeB) => List(DotAttr("shape", "square"))
      }

    def markingShapeFn[M](marking: M)(implicit markingLike: MarkingLike[M, P]): ShapeFn = node =>
      node match {
        case Left(nodeA) =>
          markingLike.multiplicity(marking).get(nodeA) match {
            case Some(n) if n > 0 =>
              List(DotAttr("shape", "doublecircle"), DotAttr("color", "darkorange"), DotAttr("penwidth", 2))
            case _ => List(DotAttr("shape", "circle"), DotAttr("color", "darkorange"), DotAttr("penwidth", 2))
          }

        case Right(nodeB) => List(DotAttr("shape", "square"), DotAttr("color", "blue4"), DotAttr("penwidth", 2))
      }

    def toDot(): String = toDotWithShapeFn(defaultShapFn)

    def toDot[M](marking: M)(implicit markingLike: MarkingLike[M, P]): String = toDotWithShapeFn(
      markingShapeFn(marking)
    )

    private def toDotWithShapeFn(shapeFn: ShapeFn): String = {

      val root = DotRootGraph(
        directed = true,
        id = Some("Process"),
        attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
        attrList = List.empty
      ) //List(DotAttr("attr_1", """"one""""), DotAttr("attr_2", "<two>")))

      def nodeId(node: BiPartiteGraph[P, T]#NodeT): String = {
        node.value match {
          case Left(place) => place.toString //place.label
          case Right(transition) => transition.toString //transition.label
        }
      }

      def myNodeTransformer(innerNode: BiPartiteGraph[P, T]#NodeT): Option[(DotGraph, DotNodeStmt)] =
        innerNode.value match {
          case Left(nodeA) => Some((root, DotNodeStmt(nodeA.toString, shapeFn(nodeA))))
          case Right(nodeB) => Some((root, DotNodeStmt(nodeB.toString, shapeFn(nodeB))))
        }

      def myEdgeTransformer(innerEdge: BiPartiteGraph[P, T]#EdgeT): Option[(DotGraph, DotEdgeStmt)] =
        innerEdge.edge match {
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
