package io.process.designer.model

import io.process.common.draw.ui.{ Movable, Pickable }
import io.process.designer.model.Graph._
import io.process.common.draw._
import io.process.common.geometry._

object Graph extends GraphVisuals {

  case class VisualNode[N](obj: N, drawing: Drawing, centre: Point)

  case class VisualEdge[E](obj: E, arrowFrom: Drawing, arrowTo: Drawing, path: List[Point] = Nil)

  case class VisualGraph[N, E](nodes: Set[VisualNode[N]], edges: Set[VisualEdge[E]])

  case class Label(text: String, font: String, offset: Point)
}

trait GraphVisuals {

  implicit def move[T]: Movable[VisualNode[T]] = new Movable[VisualNode[T]] {
    override def move(e: VisualNode[T])(p: Point) = VisualNode[T](e.obj, e.drawing, p)
  }

  implicit def draw[T]: Drawable[VisualNode[T]] = e => translate(e.centre)(e.drawing)

  implicit def pick[T]: Pickable[VisualNode[T]] = Pickable.proximityPick(10.0).rmap(e => e.centre)
}
