package io.process.geometry

case class Polygon(points: List[Point]) extends ClosedPath with Transformable[Polygon] {

  require(points.size > 2, "A polygon must have at least 3 points")

  override def apply(p: Point): Boolean = false

  override def transform(t: AffineTransform): Polygon = Polygon(points.map(p => t(p)))
}
