package io.process.designer.model

import io.process.geometry.Point

trait Movable[T] {

  def move(e: T)(p: Point): T
}
