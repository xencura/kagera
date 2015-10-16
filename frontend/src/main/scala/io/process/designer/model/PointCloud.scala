package io.process.designer.model

import io.process.common.draw._
import io.process.common.draw.ui.{ Movable, Pickable }
import io.process.common.geometry._

import scala.util.Random

object PointCloud {

  implicit def drawPoint(p: Point): Drawing = Fill("blue", Circle(5.0, p))

  implicit val pickPoint = Pickable.proximityPick(8.0)

  implicit val movePoint = new Movable[Point] {
    override def move(e: Point)(p: Point): Point = p
  }

  def pointCloud(n: Int, bounds: Rectangle) = {
    (1 until n)
      .map(i =>
        Point(Random.nextDouble() * bounds.width + bounds.point.x, Random.nextDouble() * bounds.height + bounds.point.y)
      )
      .toSet
  }
}
