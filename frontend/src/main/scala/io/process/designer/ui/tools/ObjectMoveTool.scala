package io.process.designer.ui.tools

import io.process.designer.model.Layer
import io.process.designer.ui._

class ObjectMoveTool[T](button: Int) extends MouseTool[Layer[T, _], Option[T]] {
  override def onMouseEvent = {
    case (layer, None) => pick(layer)
    case (layer, Some(e)) => moveOrDrop(layer, e)
  }

  def pick(layer: Layer[T, _]): Transform = { case MouseEvent(Down, `button`, p) =>
    (layer, layer.pick(p))
  }

  def moveOrDrop(layer: Layer[T, _], e: T): Transform = {
    case MouseEvent(Move, `button`, p) => (layer.move(e)(p), Some(e))
    case MouseEvent(Up, `button`, p) => (layer, None)
  }
}
