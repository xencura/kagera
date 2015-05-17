package io.process.designer

import io.process.designer.PetriNetModel.Place
import io.process.draw._
import io.process.geometry._
import io.process.designer.scalajs.DomEditor
import io.process.designer.ui.tools.{ ObjectInsertionTool, ObjectMoveTool, ShowCoordinatesTool }
import io.process.designer.views.Grid

import scala.scalajs.js
import js.Dynamic.{ global => g }
import org.scalajs.dom

object Test extends js.JSApp {

  implicit def drawPointSet(set: Set[Point]): Drawing = set.map(p => Fill("blue", Circle(5.0, p)))

  def main(): Unit = {

    val w = 800
    val h = 600

    val editor = new DomEditor(dom.document.getElementById("viewport"), w, h)

    editor.addLayer(Fill("#efefef", Rect(Point.origin, w, h)))
    editor.addLayer(new Grid(20, 20, w, h))

    val insert = new ObjectInsertionTool[Place](0, PetriNetModel.placeProvider)
    val move = new ObjectMoveTool[Place](0)

    val tool = insert.or(move)
    val initial = (PetriNetModel.placeLayer, (None, None))

    editor.addLayerWithTool(initial, tool)
    editor.addLayerWithTool[None.type, Drawing]((None, None), new ShowCoordinatesTool())
  }
}
