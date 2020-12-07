package io.kagera.vis.cytoscape

sealed trait NodeShape {
  val name: String
}

object NodeShape {
  case object Rectangle extends NodeShape {
    override val name = "rectangle"
  }
  case object Ellipse extends NodeShape {
    override val name = "ellipse"
  }
  case object Triangle extends NodeShape {
    override val name = "triangle"
  }
}
