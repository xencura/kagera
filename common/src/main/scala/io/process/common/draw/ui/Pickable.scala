package io.process.common.draw.ui

import io.process.common.geometry.{ Point, Rectangle }

object Pickable {
  def proximityPick(r: Double) = new Pickable[Point] {
    override def pickPoint(e: Point)(p: Point): Boolean = e ~ p < r
    override def pickRect(e: Point)(r: Rectangle): Boolean = r.apply(e)
  }
}

trait Pickable[T] {

  def pickPoint(e: T)(p: Point): Boolean
  def pickRect(e: T)(r: Rectangle): Boolean

  def rmap[M](fn: M => T): Pickable[M] = {
    val self = this
    new Pickable[M] {
      override def pickPoint(e: M)(p: Point): Boolean = self.pickPoint(fn(e))(p)
      override def pickRect(e: M)(r: Rectangle): Boolean = self.pickRect(fn(e))(r)
    }
  }
}
