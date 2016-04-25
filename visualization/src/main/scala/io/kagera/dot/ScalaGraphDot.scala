package io.kagera.dot

import io.kagera.api.MarkingLike

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._

object ScalaGraphDot {

  type DotAttrFn[T] = T => scala.Seq[DotAttr]

  type LabelFn[N] = N => String

  def biPartiteNodeId[A, B]: LabelFn[Either[A, B]] = node =>
    node match {
      case Left(a) => a.toString
      case Right(b) => b.toString
    }

  def petriNetNodeShape[P, T]: DotAttrFn[Either[P, T]] = node =>
    node match {
      case Left(nodeA) => List(DotAttr("shape", "circle"))
      case Right(nodeB) => List(DotAttr("shape", "square"))
    }

  def markingShapeFn[P, T, M](marking: M)(implicit markingLike: MarkingLike[M, P]): DotAttrFn[Either[P, T]] = node =>
    node match {
      case Left(nodeA) =>
        markingLike.multiplicity(marking).get(nodeA) match {
          case Some(n) if n > 0 =>
            List(
              DotAttr("shape", "doublecircle"),
              DotAttr("color", "darkorange"),
              DotAttr("style", "filled"),
              DotAttr("fillcolor", "darkorange"),
              DotAttr("penwidth", 2)
            )
          case _ => List(DotAttr("shape", "circle"), DotAttr("color", "darkorange"), DotAttr("penwidth", 2))
        }

      case Right(nodeB) => List(DotAttr("shape", "square"), DotAttr("color", "blue4"), DotAttr("penwidth", 2))
    }

  // TODO Generalize this for all types of graphs
  implicit class PetriNetVisualization[P, T](graph: Graph[Either[P, T], WDiEdge]) {

    def toDot[M](marking: M)(implicit markingLike: MarkingLike[M, P]): String =
      toDot("Process", biPartiteNodeId, markingShapeFn(marking))

    private def toDot(id: String, nodeIdFn: LabelFn[Either[P, T]], nodeAttrFn: DotAttrFn[Either[P, T]]): String = {

      val myRoot = DotRootGraph(
        directed = true,
        id = Some(id),
        attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
        attrList = List.empty
      )

      def myNodeTransformer(innerNode: Graph[Either[P, T], WDiEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
        Some((myRoot, DotNodeStmt(nodeIdFn(innerNode.value), nodeAttrFn(innerNode.value))))
      }

      def myEdgeTransformer(innerEdge: Graph[Either[P, T], WDiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        val source = innerEdge.edge.source.value
        val target = innerEdge.edge.target.value

        Some((myRoot, DotEdgeStmt(nodeIdFn(source), nodeIdFn(target), List.empty)))
      }

      graph2DotExport(graph).toDot(
        dotRoot = myRoot,
        edgeTransformer = myEdgeTransformer,
        cNodeTransformer = Some(myNodeTransformer)
      )
    }
  }
}
