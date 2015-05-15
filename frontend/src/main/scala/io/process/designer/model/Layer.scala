package io.process.designer.model

import io.process.draw.{ Drawable, Drawing }
import io.process.geometry.Point

/**
 * Layer trait.
 *
 * TODO Use lenses to query & update layers?
 */
trait Layer[T, Model] extends Drawing {

  def model: Model

  def objects: Set[T]

  def +(e: T, p: Point): Layer[T, Model]

  def drawable(e: T): Option[Drawing]

  def pick(point: Point): Option[T] = None

  def move(e: T, point: Point): Layer[T, Model]
}
