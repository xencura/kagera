package io.process.designer

import io.process.designer.model.Graph.VisualNode
import io.process.designer.model.{ Foo, Node, PetriNetModel }
import io.process.designer.model.PointCloud._
import io.process.designer.model.PetriNetModel._
import io.process.common.draw.ui.MouseTools.SimpleMoveTool
import io.process.designer.views.Grid
import io.process.designer.views.Grid.GridProperties
import io.process.common.draw._
import io.process.common.geometry._
import io.process.designer.scalajs.DomEditor

import scala.scalajs.js
import js.Dynamic.{ global => g }
import org.scalajs.dom

object Test extends js.JSApp {

  def main(): Unit = {

    val bounds = Rectangle((0, 0), 800, 600)

    val background: BoundedDrawable[FillStyle] = d => fillStyle => Fill(fillStyle, Rect(Point.origin, d.width, d.height))
    val drawGrid: BoundedDrawable[GridProperties] = Grid.drawGrid

    val editor = new DomEditor(dom.document.getElementById("viewport"), bounds.width.toInt, bounds.height.toInt)

    val bgNode = Node.noui[FillStyle]("#efefef")(background)

    val transitions =
      Node.set(PetriNetModel.transitions(10, bounds)).withTool(None, new SimpleMoveTool[VisualNode[Transition]](0))
    val places = Node.set(PetriNetModel.places(10, bounds)).withTool(None, new SimpleMoveTool[VisualNode[Place]](1))
    val grid = Grid(20, 20, "#ababab")

    val scene = bgNode ~> (grid ~> (places ~> transitions))

    /**
     * (pointCloud & selection) % { (p, s) => { case KeyEvent(Down, 'DELETE') => (p - s, Set.empty) } } * }
     */

    editor.setScene(scene)
  }
}
