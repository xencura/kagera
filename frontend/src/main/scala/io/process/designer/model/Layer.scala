package io.process.designer.model

import io.process.draw.{ Drawable, Drawing }
import io.process.geometry.{ Point, Rectangle }

/**
 * 'Layer' trait, collects some useful visual manipulation functions.
 *
 * consider using Pickable, Moveable instead?
 */
trait Layer[T, M <: Iterable[T]] extends Drawing {

  def objects: M

  def +(e: T)(point: Point): Layer[T, M]

  def drawable(e: T): Option[Drawing]

  def pick(p: Point): Option[T] = None

  def pick(r: Rectangle): Iterable[T]

  def move(e: T)(point: Point): Layer[T, M]
}
