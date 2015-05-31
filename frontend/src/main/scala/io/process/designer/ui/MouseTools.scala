package io.process.designer.ui

import io.process.designer.model.{ Movable, Pickable }
import io.process.designer.ui._
import io.process.draw._
import io.process.geometry.{ AffineTransform, Point }

import scalaz.State

object MouseTools {

  def moveTool[T](implicit pickFn: Pickable[T], moveFn: Movable[T]): UITool[Set[T], Option[T]] = {
    case None => pick
    case Some(e) => move(e) | drop(e) | reset(None)
  }

  def reset[M, T](s: T): Action[M, T] = { case FocusLost => State { m => (m, s) } }

  def pick[T](implicit fn: Pickable[T]): Action[Set[T], Option[T]] = { case MouseEvent(MouseDown, button, p, _) =>
    State { m => (m, m.find(e => fn.pick(e, p))) }
  }

  def move[T](e: T)(implicit moveFn: Movable[T]): Action[Set[T], Option[T]] = {
    case MouseEvent(MouseMove, button, p, _) =>
      val moved = moveFn.move(e)(p)
      State { m => (m - e + moved, Some(moved)) }
  }

  def drop[T](e: T): Action[Set[T], Option[T]] = { case MouseEvent(MouseUp, button, p, _) =>
    State { m => (m, None) }
  }

  def insert[T](e: T): Action[Set[T], Option[T]] = { case MouseEvent(MouseUp, button, p, _) =>
    State { m => (m + e, None) }
  }

  def showCoordinates[M](fillStyle: FillStyle): Action[M, Drawing] = { case MouseEvent(t, button, p, mods) =>
    val pr = p.round
    State { m => (m, fill(fillStyle, Text(s"${pr.x},${pr.y}", p))) }
  }

  //  def panTool[T](button: Int): UITool[Set[T], Option[Point]] = {
  //    case None    => pick
  //    case Some(e) => pan(e) | reset(None)
  //  }
  //
  //  def start(button: Int): Action[AffineTransform, Point] {
  //
  //  }
  //
  //  def pan(button: Int): Action[AffineTransform, AffineTransform] = {
  //    case MouseEvent(MouseDown, `button`, p, mods) => State { t => (m + e, None) }
  //  }
}
