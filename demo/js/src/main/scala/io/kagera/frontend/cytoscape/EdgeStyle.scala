package io.kagera.frontend.cytoscape

object EdgeStyle {
  val default = EdgeStyle()
}

case class EdgeStyle(
  width: Int = 3,
  curveStyle: CurveStyle = CurveStyle.Haystack,
  lineColor: String = "black",
  sourceArrow: ArrowStyle = ArrowStyle.default,
  midSourceArrow: ArrowStyle = ArrowStyle.default,
  midTargetArrow: ArrowStyle = ArrowStyle.default,
  targetArrow: ArrowStyle = ArrowStyle.default
)
