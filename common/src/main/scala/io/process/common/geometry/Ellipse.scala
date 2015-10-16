package io.process.common.geometry

case class Ellipse(centre: Point, radius: Point, angle: Double) extends ClosedPath with Transformable[Ellipse] {

  override def apply(p: Point) = false

  override def transform(t: AffineTransform): Ellipse = ???
}
