package io.process.common

package object geometry {

  type LineSegment = (Point, Point)
  type ClosedPath = Point => Boolean

  implicit def intTupletoPoint(tuple: (Int, Int)) = Point(tuple._1, tuple._2)
  implicit def doubleTupletoPoint(tuple: (Double, Double)) = Point(tuple._1, tuple._2)
}
