package io.process.common.draw.ui

import io.process.common.draw._
import io.process.common.geometry.{ AffineTransform, Point }
import scala.collection.generic.CanBuildFrom
import scalaz._

object MouseTools {

  class SimpleMoveTool[T : Pickable : Movable](button: Int) extends DragTool[Set[T], T](button) {
    override def pick = s => p => s.find(e => implicitly[Pickable[T]].pickPoint(e)(p))
    override def drop = (set, e) => (from, to) => drag(set, e)(from, to)._1
    override def drag = (set, e) =>
      (from, to) => {
        val moved = implicitly[Movable[T]].move(e)(to)
        (set - e + moved, moved)
      }
  }

  class PanTool(button: Int) extends DragTool[AffineTransform, Point](button) {
    override def pick = s => p => Some(Point.origin)
    override def drop = (t, e) => (from, to) => drag(t, e)(from, to)._1
    override def drag = (t, e) =>
      (from, to) => {
        val drag = from - to
        (t.translate(drag), drag)
      }
  }

  def showCoordinates[M](fillStyle: FillStyle): Action[M, Drawing] = { case MouseEvent(_, _, p, _) =>
    val pr = p.round
    State { m => (m, fill(fillStyle, Text(s"${pr.x},${pr.y}", p))) }
  }

  def insertTool[T, C <: Set[T]](button: Int)(fn: Point => T)(implicit
    builder: CanBuildFrom[Set[T], T, C]
  ): UIHandler[C] = (elements: C) => { case MouseEvent(MouseDown, `button`, p, mods) =>
    elements.++[T, C](Seq(fn(p)))
  }
}
