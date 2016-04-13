package io.process.designer

import io.process.common.draw._
import org.scalajs.dom
import org.w3c.dom.events.UIEvent

import scalaz.Functor

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
        case ArcTo(p1, p2, r) => gc.arcTo(p1.x, p1.y, p2.x, p2.y, r)
        case BezierCurveTo(cp1, cp2, p) => gc.bezierCurveTo(cp1.x, cp1.y, cp2.x, cp2.y, p.x, p.y)
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
        case Clip(rect, drawing) =>
          gc.save()
          gc.addPath(Rect(rect.point, rect.width, rect.height))
          gc.clip()
          gc.draw(drawing)
          gc.restore()
        case ClearRect(rect) =>
          gc.clearRect(rect.x, rect.y, rect.width, rect.height)
      }
    }
  }

  class DomCanvas(canvas: dom.html.Canvas) extends Canvas {
    override def draw[T : Drawable](drawable: T): Unit = canvas.context.draw(drawable)
    override def height: Int = canvas.height
    override def width: Int = canvas.width
  }

  implicit class CanvasWithAdditions(val canvas: GraphicsCanvas) extends AnyVal {
    def context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
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
