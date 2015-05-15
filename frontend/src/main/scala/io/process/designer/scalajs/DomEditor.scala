package io.process.designer.scalajs

import io.process.common.EditState
import io.process.draw._
import io.process.designer.ui._

import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }

case class Layer[T : Drawable, S : Drawable](
  canvas: dom.html.Canvas,
  model: T,
  tool: MouseTool[T, S],
  updateFn: Option[T => Any]
) extends EditState[(T, Option[S])] {

  override def initialState = (model, None)
  override def onChange(from: (T, Option[S]), to: (T, Option[S])): Unit = {
    if (from._1 != to._1)
      canvas.clearAndDraw(to._1)
  }

  def onMouseEvent(e: MouseEvent) = apply { s => tool.onMouseEvent.lift(e, s).getOrElse(s) }
}

class DomEditor(parent: dom.Element, val width: Int, val height: Int) {

  var layers: List[Layer[_, _]] = List.empty
  val toolLayer = addCanvas(20)

  Seq("mousemove" -> Move, "mouseup" -> Up, "mousedown" -> Down).foreach { case (name, event) =>
    parent.addEventListener(name, mouseListener(event))
  }

  def mouseListener(t: EventType) = (e: dom.MouseEvent) => {
    layers.foreach { layer => layer.onMouseEvent(MouseEvent(t, e.button, layer.canvas.toCanvasPoint(e))) }
  }

  def addLayerWithTool[T : Drawable, S : Drawable](model: T, tool: MouseTool[T, S]) = addLayer(model, Some(tool))

  def addLayer[T : Drawable, S : Drawable](
    model: T,
    tool: Option[MouseTool[T, S]] = None,
    fn: Option[T => Any] = None
  ) = {
    val canvas = addCanvas(parent.childElementCount + 1)
    canvas.context.draw(model)
    layers = new Layer[T, S](canvas, model, tool.getOrElse(MouseTool.nothing()), fn) :: layers
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
