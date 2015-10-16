package io.process.common.geometry

case class Circle(r: Double, centre: Point = Point.origin) extends ClosedPath with Transformable[Ellipse] {

  override def apply(p: Point) = p ~ centre < r

  override def transform(t: AffineTransform): Ellipse = ???
}
