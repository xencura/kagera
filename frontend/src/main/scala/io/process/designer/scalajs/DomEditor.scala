package io.process.designer.scalajs

import io.process.common.EditState
import io.process.draw._
import io.process.designer.ui._

import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }

case class Node[M : Drawable, S](canvas: dom.html.Canvas, initialState: (M, S), tool: MouseTool[M, S])
    extends EditState[(M, S)] {

  override def onChange(from: (M, S), to: (M, S)): Unit = {
    canvas.clearAndDraw(to._1)
  }

  def onMouseEvent(e: MouseEvent) = {
    tool.onMouseEvent.tupled(state).lift(e).foreach(update => apply(s => update))
  }
}

class DomEditor(parent: dom.Element, val width: Int, val height: Int) {

  var layers: List[Node[_, _]] = List.empty
  val toolLayer = addCanvas(20)

  Seq("mousemove" -> Move, "mouseup" -> Up, "mousedown" -> Down).foreach { case (name, event) =>
    parent.addEventListener(name, mouseListener(event))
  }

  def mouseListener(t: EventType) = (e: dom.MouseEvent) => {
    g.console.log(e.button)
    layers.foreach { layer => layer.onMouseEvent(MouseEvent(t, e.button, layer.canvas.toCanvasPoint(e))) }
  }

  def addLayer[M : Drawable](model: M) = addLayerWithTool((model, None), MouseTool.nothing())

  def addLayerWithTool[M : Drawable, S](model: (M, S), tool: MouseTool[M, S]) = {
    val canvas = addCanvas(parent.childElementCount + 1)
    canvas.context.draw(model._1)
    layers = new Node[M, S](canvas, model, tool) :: layers
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
