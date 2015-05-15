package io.process.draw

import io.process.geometry.Dimensions

trait Canvas extends Dimensions {

  def draw[T : Drawable](drawable: T)
}
