package io.process.designer.model

import io.process.draw._
import io.process.geometry._

object PointCloud {

  implicit def drawPoint(p: Point): Drawing = Fill("blue", Circle(5.0, p))

  implicit val pickPoint = new Pickable[Point] {
    override def pick(e: Point, p: Point): Boolean = e ~ p < 8.0
    override def pick(e: Point, r: Rectangle): Boolean = r.contains(e)
  }

  implicit val movePoint = new Movable[Point] {
    override def move(e: Point)(p: Point): Point = p
  }
}
