package io.process.designer.ui.tools

import io.process.designer.model.Layer
import io.process.designer.ui._

class ObjectMoveTool[T](button: Int) extends MouseTool[Layer[T, _], T] {

  // format: OFF
  override def onMouseEvent = {
    case (MouseEvent(Down, `button`, p), (layer, None))    => (layer, layer.pick(p))
    case (MouseEvent(Up,   `button`, p), (layer, Some(e))) => (layer, None)
    case (MouseEvent(Move, `button`, p), (layer, Some(e))) => (layer.move(e, p), Some(e))
  }
  // format: ON
}
