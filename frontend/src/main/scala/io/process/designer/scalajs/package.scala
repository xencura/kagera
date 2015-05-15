package io.process.designer

import io.process.draw._
import org.scalajs.dom

package object scalajs {

  type GraphicsCanvas = org.scalajs.dom.html.Canvas
  type GraphicsContext = org.scalajs.dom.CanvasRenderingContext2D

  implicit class GraphicsContextWithAdditions(gc: GraphicsContext) {

    def addPath(path: Path) = {
      gc.beginPath()
      path.foreach {
        case MoveTo(p) => gc.moveTo(p.x, p.y)
        case LineTo(p) => gc.lineTo(p.x, p.y)
        case Arc(c, r, sa, ea) => gc.arc(c.x, c.y, r, sa, ea)
        case Rect(p, w, h) => gc.rect(p.x, p.y, w, h)
        case Text(text, p) => gc.fillText(text, p.x, p.y)
      }
      gc.closePath()
    }

    def draw(drawing: Drawing): Unit = {
      drawing.foreach {
        case Transform(t, d) =>
          gc.save()
          (gc.transform _ tupled)(t.m)
          gc.draw(d)
          gc.restore()
        case Fill(style, path) =>
          gc.fillStyle = style
          addPath(path)
          gc.fill()
        case Stroke(style, path) =>
          gc.strokeStyle = style
          addPath(path)
          gc.stroke()
      }
    }
  }

  class DomCanvas(canvas: dom.html.Canvas) extends Canvas {

    override def draw[T : Drawable](drawable: T): Unit = canvas.context.draw(drawable)
    override def height: Int = canvas.height
    override def width: Int = canvas.width
  }

  implicit class CanvasWithAdditions(canvas: GraphicsCanvas) {
    lazy val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    def clear() = context.clearRect(0, 0, canvas.width, canvas.height)
    def clearAndDraw(d: Drawing) = {
      clear()
      context.draw(d)
    }

    def toCanvasPoint(e: dom.MouseEvent) = {
      val rect = canvas.getBoundingClientRect()
      val x = (e.clientX - rect.left) / (rect.right - rect.left) * canvas.width
      val y = (e.clientY - rect.top) / (rect.bottom - rect.top) * canvas.height
      (x, y)
    }
  }
}
