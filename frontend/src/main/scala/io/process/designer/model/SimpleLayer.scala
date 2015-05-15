package io.process.designer.model

import io.process.draw._
import io.process.geometry._

class SimpleLayer[T](locations: Map[T, Point], template: Drawing) extends Layer[T, Set[T]] {

  val r = 10.0

  override val objects = locations.keySet
  override val model = locations.keySet

  override def drawable(e: T): Option[Drawing] = locations.get(e).map(p => drawObj(e, p))

  override def iterator = locations.map { case (e, p: Point) => drawObj(e, p) }.flatten.iterator

  override def move(e: T, point: Point): Layer[T, Set[T]] =
    if (locations.contains(e))
      this + (e, point)
    else this

  override def pick(p: Point) = locations.filter { case (e, ep: Point) => ep ~ p < r }.headOption.map(_._1)

  def drawObj(e: T, p: Point): Drawing = translate(p.x, p.y)(template)

  override def +(e: T, point: Point): Layer[T, Set[T]] = new SimpleLayer[T](locations + (e -> point), template)
}
