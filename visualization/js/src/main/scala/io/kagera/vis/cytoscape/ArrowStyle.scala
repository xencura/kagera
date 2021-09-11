package io.kagera.vis.cytoscape

import io.kagera.vis.cytoscape.ArrowShape.NoArrow

object ArrowStyle {
  val default = ArrowStyle()
}

case class ArrowStyle(color: String = "black", shape: ArrowShape = NoArrow, fill: String = "filled")
