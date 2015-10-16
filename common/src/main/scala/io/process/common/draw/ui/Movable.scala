package io.process.common.draw.ui

import io.process.common.geometry.Point

trait Movable[T] {

  def move(e: T)(p: Point): T
}
