package io.kagera.vis.cytoscape

import io.kagera.vis.cytoscape.NodeShape.Ellipse

object NodeStyle {
  val default = NodeStyle(20, 20, Ellipse, "grey")
}

case class NodeStyle(width: Int, height: Int, shape: NodeShape = NodeShape.Rectangle, backgroundColor: String = "grey")
