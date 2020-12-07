package io.kagera.vis.cytoscape

sealed trait ArrowShape {
  val name: String
}

object ArrowShape {

  case object Triangle extends ArrowShape {
    override val name = "triangle"
  }

  case object NoArrow extends ArrowShape {
    override val name = "none"
  }
}
