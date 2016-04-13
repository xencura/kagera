package io.process.common.draw.ui

import io.process.common.geometry._

import scalaz._

abstract class DragTool[S, T](button: Int) extends UITool[S, Option[(Point, T)]] {

  override def apply(s: Option[(Point, T)]): Action[S, Option[(Point, T)]] = s match {
    case None => { case MouseEvent(MouseDown, `button`, p, _) =>
      State { m => (m, pick(m)(p).map(e => (p, e))) }
    }
    case Some((start, e)) => {
      case MouseEvent(MouseMove, `button`, p, _) =>
        State { m =>
          val result = drag(m, e)(start, p)
          (result._1, Some(start, result._2))
        }
      case MouseEvent(MouseUp, `button`, p, _) => State { m => (drop(m, e)(start, p), None) }
      case FocusLost => State { m => (m, None) }
    }
  }

  def pick: S => Point => Option[T]
  def drag: (S, T) => (Point, Point) => (S, T)
  def drop: (S, T) => (Point, Point) => S
}
