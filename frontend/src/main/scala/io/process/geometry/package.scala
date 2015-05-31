package io.process

package object geometry {

  type LineSegment = (Point, Point)
  type ClosedPath = Point => Boolean

  implicit def intTupletoPoint(tuple: (Int, Int)) = Point(tuple._1, tuple._2)
  implicit def doubleTupletoPoint(tuple: (Double, Double)) = Point(tuple._1, tuple._2)

  case class Circle(r: Double, centre: Point = Point.origin) extends ClosedPath {
    override def apply(p: Point) = p ~ centre < r
  }
}
