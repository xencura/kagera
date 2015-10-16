package io.process.designer.scalajs

import io.process.designer.model.Node
import io.process.common.draw.ui._
import io.process.common.geometry.Dimensions

import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }

class DomEditor(parent: dom.Element, val width: Int, val height: Int) extends Dimensions {

  var layers: List[(Node[_, _], dom.html.Canvas)] = List.empty
  val toolLayer = addCanvas(20)

  Seq("mousemove" -> MouseMove, "mouseup" -> MouseUp, "mousedown" -> MouseDown).foreach { case (name, event) =>
    parent.addEventListener(name, mouseListener(event))
  }

  parent.addEventListener("wheel", wheelListener _)

  def wheelListener(e: dom.WheelEvent) = {
    g.console.log(s"$e")
  }

  def mouseListener(t: MouseEventType) = (e: dom.MouseEvent) => {
    layers = layers.map { case (node, canvas) =>
      (node.onEvent(MouseEvent(t, e.button, canvas.toCanvasPoint(e), 0)), canvas)
    }
    layers.foreach { case (node, canvas) =>
      canvas.clear()
      node.seq.foreach(n => canvas.context.draw(n.draw(this)))
    }
  }

  def setScene[M, S](node: Node[M, S]) = {
    val canvas = addCanvas(parent.childElementCount + 1)
    canvas.clearAndDraw(node.draw(this))
    layers = (node, canvas) :: layers
  }

  private def addCanvas(zindex: Int) = {
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.html.Canvas]
    parent.appendChild(canvas)
    canvas.width = width
    canvas.height = height
    canvas.setAttribute("style", s"position: absolute; left: 0; top: 0; z-index: $zindex")
    canvas
  }
}
