package io.process.designer.ui.tools

import io.process.designer.model.Layer
import io.process.designer.ui._

class ObjectInsertionTool[T](button: Int, provider: () => T) extends MouseTool[Layer[T, _]] {
  override def onMouseEvent = (layer: Layer[T, _]) => {
    case (MouseEvent(Down, `button`, p)) if layer.pick(p).isEmpty => layer.+(provider())(p)
  }
}
