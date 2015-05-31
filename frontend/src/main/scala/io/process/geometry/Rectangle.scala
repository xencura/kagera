package io.process.geometry

case class Rectangle(point: Point, val w: Double, val h: Double) extends Dimensions {

  require(width > 0, "The width of a rectangle must be greater then 0")
  require(height > 0, "The height of a rectangle must be greater then 0")

  def contains(p: Point): Boolean = {
    p.x >= point.x && p.y >= point.y && p.x <= p.x + w && p.y <= p.y + h
  }

  override def width: Int = w.toInt
  override def height: Int = h.toInt
}
