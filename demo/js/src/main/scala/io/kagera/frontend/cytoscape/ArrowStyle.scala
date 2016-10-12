package io.kagera.frontend.cytoscape

import io.kagera.frontend.cytoscape.ArrowShape.NoArrow

object ArrowStyle {
  val default = ArrowStyle()
}

case class ArrowStyle(color: String = "black", shape: ArrowShape = NoArrow, fill: String = "filled")
