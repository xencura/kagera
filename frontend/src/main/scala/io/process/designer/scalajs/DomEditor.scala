package io.process.designer.scalajs

import io.process.common.EditState
import io.process.draw._
import io.process.designer.ui._

import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }

case class Node[M : Drawable](canvas: dom.html.Canvas, model: M, tool: Option[MouseTool[M]]) extends EditState[M] {

  override def initialState = model
  override def onChange(from: M, to: M): Unit = {
    canvas.clearAndDraw(to)
  }

  def onMouseEvent(e: MouseEvent) = apply { s => tool.map(t => t.onMouseEvent(s).lift(e)).flatten.getOrElse(s) }
}

class DomEditor(parent: dom.Element, val width: Int, val height: Int) {

  var layers: List[Node[_]] = List.empty
  val toolLayer = addCanvas(20)

  Seq("mousemove" -> Move, "mouseup" -> Up, "mousedown" -> Down).foreach { case (name, event) =>
    parent.addEventListener(name, mouseListener(event))
  }

  def mouseListener(t: EventType) = (e: dom.MouseEvent) => {
    g.console.log(e.button)
    layers.foreach { layer => layer.onMouseEvent(MouseEvent(t, e.button, layer.canvas.toCanvasPoint(e))) }
  }

  def addLayerWithTool[M : Drawable](model: M, tool: MouseTool[M]) = addLayer(model, Some(tool))

  def addLayer[M : Drawable](model: M, tool: Option[MouseTool[M]] = None) = {
    val canvas = addCanvas(parent.childElementCount + 1)
    canvas.context.draw(model)
    layers = new Node[M](canvas, model, tool) :: layers
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
