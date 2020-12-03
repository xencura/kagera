package io.kagera.frontend.cytoscape

import io.kagera.frontend.cytoscape.NodeShape.Ellipse

object NodeStyle {
  val default = NodeStyle(20, 20, Ellipse, "grey")
}

case class NodeStyle(width: Int, height: Int, shape: NodeShape = NodeShape.Rectangle, backgroundColor: String = "grey")
