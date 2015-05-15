package io.process.designer.views

import io.process.draw._
import io.process.geometry._

class Grid(gridX: Int, gridY: Int, width: Int, height: Int) extends Drawing {

  def gridStrokeStyle = "#bbbbbf"
  def snapToIS(point: Point): Point = Point(roundToFactor(point.x, gridX), roundToFactor(point.y, gridY))
  def roundToFactor(n: Double, factor: Long): Long = math.round(n / factor) * factor

  // TODO this seems too verbose
  val xlines = (1 until (width / gridX)).map(gridX *).map { x => MoveTo(x, 0) ~> LineTo(x, height) }.flatten
  val ylines = (1 until (height / gridY)).map(gridY *).map { y => MoveTo(0, y) ~> LineTo(width, y) }.flatten
  val grid: Drawing = translate(0.5, 0.5) { Stroke(gridStrokeStyle, xlines ++ ylines) }

  override def iterator = grid.iterator
}
