package io.process.designer

import io.process.designer.model.Node
import io.process.designer.model.PointCloud._
import io.process.designer.ui.MouseTools
import io.process.designer.views.Grid
import io.process.draw._
import io.process.geometry._
import io.process.designer.scalajs.DomEditor

import scala.scalajs.js
import js.Dynamic.{ global => g }
import org.scalajs.dom

object Test extends js.JSApp {

  def main(): Unit = {

    val editor = new DomEditor(dom.document.getElementById("viewport"), 800, 600)

    val background = Node.noui[FillStyle]("#efefef")(d => fill => Fill(fill, Rect(Point.origin, d.width, d.height)))

    val nodes = Node[Set[Point], Option[Point]](
      state = (Set(Point(10, 10), Point(50, 50)), None),
      tool = MouseTools.moveTool,
      drawFn = d => s => drawIterable[Point].apply(s._1)
    )

    val scene = background ~> (Grid(20, 20, "#ababab") ~> nodes)

    editor.setScene(scene)
  }
}
