package io.kagera.vis.cytoscape

import io.kagera.api.colored.ColoredPetriNet
import org.scalajs.dom.html.Div

object CytoScapePetriNetVisualization {

  def drawPetriNet[P, T](graphContainer: Div, pn: ColoredPetriNet) = {
    val nodes = pn.nodes.map {
      case Left(p) =>
        Node(p.toString, NodeStyle(width = 30, height = 30, shape = NodeShape.Ellipse, backgroundColor = "blue"))
      case Right(t) =>
        Node(t.toString, NodeStyle(width = 30, height = 30, shape = NodeShape.Rectangle, backgroundColor = "red"))
    }.toSet

    val edges = pn.edges.zipWithIndex.map { case (e, i) =>
      Edge(
        i.toString,
        e.source.toString,
        e.target.toString,
        EdgeStyle(
          width = 2,
          curveStyle = CurveStyle.Bezier,
          lineColor = "black",
          targetArrow = ArrowStyle(color = "black", shape = ArrowShape.Triangle)
        )
      )
    }

    CytoScape.drawGraph(graphContainer, nodes, edges)
  }
}
