package io.process.common.draw

import io.process.common.geometry.Dimensions

trait Canvas extends Dimensions {

  def draw[T : Drawable](drawable: T)
}
