package io.process.designer.ui.tools

import io.process.designer.model.Layer
import io.process.designer.ui._

class ObjectInsertionTool[T](provider: () => T) extends StateLessMouseTool[Layer[T, _]] {

  override def updateModel = {
    case (MouseEvent(Down, button, p), layer) if layer.pick(p).isEmpty => layer + (provider(), p)
  }
}
