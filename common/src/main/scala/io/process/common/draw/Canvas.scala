package io.process.common.draw

import io.process.common.draw.ui.UIHandler
import io.process.common.geometry.Dimensions

trait Canvas extends Dimensions {

  def draw[T : Drawable](drawable: T)

  def addLayer(d: Drawing, handler: UIHandler = Map.empty)
}
