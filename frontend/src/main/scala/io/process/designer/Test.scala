package io.process.designer

import io.process.common.draw._
import io.process.common.draw.ui._
import io.process.common.geometry._
import io.process.designer.model.{ Layer, PointCloud }
import io.process.designer.scalajs.DomEditor
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ global => g }

object Test extends js.JSApp {

  def main(): Unit = {

    val bounds = Rectangle((0, 0), 800, 600)

    val background: BoundedDrawable[FillStyle] = d => fillStyle => Fill(fillStyle, Rect(Point.origin, d.width, d.height))

    val viewPort = dom.document.getElementById("viewport")
    val editor = new DomEditor(viewPort, bounds.width.toInt, bounds.height.toInt)

    val bgLayer: Layer[FillStyle] = Layer(background) % (s => { case MouseEvent(MouseDown, button, _, _) =>
      println(button); s
    })

    import PointCloud._
    val pointCloud = PointCloud.pointCloud(10, bounds)
    val pointCloudDrawable = implicitly[Drawable[Set[Point]]]
    val pointCloudLayer = Layer[Set[Point]](d => pointCloudDrawable)

    editor.addLayer("grey", bgLayer)
    editor.addLayer(
      pointCloud,
      Layer[Set[Point]](d => pointCloudDrawable, MouseTools.insertTool[Point, Set[Point]](0)(p => p))
    )

  }
}
