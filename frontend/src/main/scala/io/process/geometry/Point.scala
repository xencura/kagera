package io.process.geometry

object Point {
  val origin = Point(0, 0)
}

case class Point(x: Double, y: Double) {
  private def transformXY(fn: Double => Double) = Point(fn(x), fn(y))

  def abs = transformXY(math.abs)
  def round = Point(math.round(x), math.round(y))
  def *(factor: Double) = transformXY(factor *)
  def +(other: Point) = Point(x + other.x, y + other.y)
  def -(other: Point) = Point(x - other.x, y - other.y)

  def ~(other: Point): Double = { // euclidean distance
    val d = this - other
    math.sqrt(d.x * d.x + d.y * d.y)
  }

  def asTuple = (x, y)
}
