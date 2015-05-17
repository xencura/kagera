package io.process.designer.model

import io.process.draw.{ Drawable, Drawing }
import io.process.geometry.Point

/**
 * 'Layer' trait, collects some useful visual manipulation functions.
 */
trait Layer[T, Model] extends Drawing {

  def model: Model

  def +(e: T)(point: Point): Layer[T, Model]

  def drawable(e: T): Option[Drawing]

  def pick(point: Point): Option[T] = None

  def move(e: T)(point: Point): Layer[T, Model]
}
