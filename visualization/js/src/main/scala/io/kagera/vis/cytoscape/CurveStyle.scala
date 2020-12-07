package io.kagera.vis.cytoscape

sealed trait CurveStyle {
  val name: String
}

object CurveStyle {
  case object Haystack extends CurveStyle {
    override val name = "haystack"
  }

  case object Bezier extends CurveStyle {
    override val name = "bezier"
  }
}
