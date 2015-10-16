package io.process.common.draw.ui

import io.process.common.draw._
import io.process.common.geometry.{ AffineTransform, Point }
import scalaz._

object MouseTools {

  class SimpleMoveTool[T](button: Int)(implicit pickFn: Pickable[T], moveFn: Movable[T])
      extends DragTool[Set[T], T](button) {
    override def pick = s => p => s.find(e => pickFn.pickPoint(e)(p))
    override def drop = (set, e) => (from, to) => drag(set, e)(from, to)._1
    override def drag = (set, e) =>
      (from, to) => {
        val moved = moveFn.move(e)(to)
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
}
