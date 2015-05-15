package io.process.designer.ui.tools

import io.process.draw._
import io.process.designer.ui._

class ShowCoordinatesTool extends StateLessMouseTool[Drawing] {

  def fillStyle = "black"

  override def updateModel = { case (e, _) =>
    val pr = e.location.round
    Fill(fillStyle, Text(s"${pr.x},${pr.y}", e.location))
  }
}
