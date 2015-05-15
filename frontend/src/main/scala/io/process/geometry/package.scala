package io.process

package object geometry {

  type LineSegment = (Point, Point)
  type Rectangle = (Point, Double, Double)

  implicit def intTupletoPoint(tuple: (Int, Int)) = Point(tuple._1, tuple._2)
  implicit def doubleTupletoPoint(tuple: (Double, Double)) = Point(tuple._1, tuple._2)

  trait ClosedPath {
    def contains(p: Point)
  }

  case class Circle(r: Double, centre: Point = Point.origin) extends ClosedPath {
    override def contains(p: Point) = p ~ centre < r
  }
}
