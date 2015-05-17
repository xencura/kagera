package io.process.designer.ui.tools

import io.process.draw._
import io.process.designer.ui._

class ShowCoordinatesTool extends MouseTool[Drawing] {

  def fillStyle = "black"

  override def onMouseEvent = state => { case MouseEvent(t, button, p) =>
    val pr = p.round
    Fill(fillStyle, Text(s"${pr.x},${pr.y}", p))
  }
}
