package io.process.designer.ui.tools

import io.process.draw._
import io.process.designer.ui._

class ShowCoordinatesTool[T] extends MouseTool[T, Drawing] {

  def fillStyle = "black"

  override def onMouseEvent = (model, state) => { case MouseEvent(t, button, p) =>
    val pr = p.round
    (model, Fill(fillStyle, Text(s"${pr.x},${pr.y}", p)))
  }
}
