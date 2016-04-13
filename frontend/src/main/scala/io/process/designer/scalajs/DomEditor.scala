package io.process.designer.scalajs

import io.process.common.draw.ui._
import io.process.common.geometry.Dimensions
import io.process.designer.model.Layer
import org.scalajs.dom

import scala.scalajs.js.Dynamic.{ global => g }

class DomEditor(parent: dom.Element, val width: Int, val height: Int) extends Dimensions {

  case class DomLayer[S](state: S, layer: Layer[S], canvas: dom.html.Canvas)

  var layers: List[DomLayer[_]] = List.empty
  val toolLayer = addCanvas(20)

  Seq("mousemove" -> MouseMove, "mouseup" -> MouseUp, "mousedown" -> MouseDown).foreach { case (name, event) =>
    parent.addEventListener(name, mouseListener(event))
  }

  parent.addEventListener("wheel", wheelListener _)

  def wheelListener(e: dom.WheelEvent) = {
    g.console.log(s"$e")
  }

  def mouseListener(t: MouseEventType) = (e: dom.MouseEvent) => {
    layers = layers.map { case DomLayer(state, layer, canvas) =>
      val event = MouseEvent(t, e.button, canvas.toCanvasPoint(e), 0)
      val newState = layer
        .ui(state)
        .lift(event)
        .map { updatedState =>
          canvas.clear()
          canvas.context.draw(layer.draw(this)(updatedState))
          updatedState
        }
        .getOrElse(state)

      DomLayer(newState, layer, canvas)
    }
  }

  def addLayer[S](model: S, layer: Layer[S]) = {
    val canvas = addCanvas(parent.childElementCount + 1)
    canvas.clearAndDraw(layer.draw(this)(model))
    layers = DomLayer(model, layer, canvas) :: layers
  }

  private def addCanvas(zindex: Int) = {
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.html.Canvas]
    parent.appendChild(canvas)
    canvas.width = width
    canvas.height = height
    canvas.setAttribute("style", s"position: absolute; left: 0; top: 0; z-index: $zindex")
    canvas.setAttribute("tabindex", "0")
    canvas
  }
}
