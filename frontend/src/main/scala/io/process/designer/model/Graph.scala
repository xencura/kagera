package io.process.designer.model

import io.process.draw._
import io.process.geometry.Point

object Graph {

  case class VisualNode[N](obj: N, drawing: Drawing, location: Point)

  case class VisualEdge[E](obj: E)

  case class VisualGraph[N, E](nodes: Set[VisualNode[N]], edges: Set[VisualEdge[E]])

  case class Label(text: String, font: String, offset: Point)

  case class Documentation(text: String)
}
