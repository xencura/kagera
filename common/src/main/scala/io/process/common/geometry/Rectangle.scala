package io.process.common.geometry

case class Rectangle(point: Point, width: Double, height: Double) extends ClosedPath with Transformable[Polygon] {

  require(width > 0, "The width of a rectangle must be greater then 0")
  require(height > 0, "The height of a rectangle must be greater then 0")

  def x = point.x
  def y = point.y
  def apply(p: Point): Boolean = p.x >= point.x && p.y >= point.y && p.x <= p.x + width && p.y <= p.y + height
  def corners =
    (point, Point(point.x + width, point.y), Point(point.x, point.y + height), Point(point.x + width, point.y + height))

  override def transform(t: AffineTransform): Polygon = corners match {
    case (a, b, c, d) => Polygon(List(a, b, c, d))
  }
}
