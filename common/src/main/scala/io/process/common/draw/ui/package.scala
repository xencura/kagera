package io.process.common
package draw

import io.process.common.geometry._

import scalaz._

package object ui {

  sealed trait UIEvent

  trait CursorEvent {
    def location: Point
  }

  case object FocusLost extends UIEvent

  sealed trait MouseEventType
  case object MouseUp extends MouseEventType
  case object MouseDown extends MouseEventType
  case object MouseMove extends MouseEventType

  case class MouseEvent(eventType: MouseEventType, button: Int, location: Point, keyModifiers: Int)
      extends UIEvent
      with Transformable[MouseEvent] {
    override def transform(t: AffineTransform): MouseEvent =
      MouseEvent(eventType, button, t.apply(location), keyModifiers)
  }

  case class WheelEvent(deltaX: Double, deltaY: Double, deltaZ: Double, location: Point)
      extends UIEvent
      with CursorEvent

  sealed trait KeyEventType
  case object KeyDown extends KeyEventType
  case object KeyUp extends KeyEventType

  case class KeyboardEvent(eventType: KeyEventType, key: Int, keyModifiers: Int) extends UIEvent

  def actionFn[M, S](pf: PartialFunction[UIEvent, M => (M, S)]): Action[M, S] = pf.andThen(fn => State { s: M => fn(s) })

  // bridge
  type UIHandler = UIEvent ?=> Drawing

  // stateless ui action
  type Action[M, S] = UIEvent ?=> State[M, S]
  type MouseAction[M, S] = MouseEvent ?=> State[M, S]

  // a tool that requires some input state
  type UITool[M, S] = S => Action[M, S]

  implicit class ActionFunctions[M, S](action: Action[M, S]) {

    def |(other: Action[M, S]) = action.orElse(other)
  }
}
