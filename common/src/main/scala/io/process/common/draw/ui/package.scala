package io.process.common
package draw

import io.process.common.geometry._

import scalaz._

package object ui {

  // user interface event
  sealed trait UIEvent

  trait CursorEvent extends UIEvent {
    def location: Point
  }

  case object FocusLost extends UIEvent

  sealed trait MouseEventType
  case object MouseUp extends MouseEventType
  case object MouseDown extends MouseEventType
  case object MouseMove extends MouseEventType

  case class MouseEvent(eventType: MouseEventType, button: Int, location: Point, keyModifiers: Int)
      extends CursorEvent
      with Transformable[MouseEvent] {
    override def transform(t: AffineTransform): MouseEvent =
      MouseEvent(eventType, button, t.apply(location), keyModifiers)
  }

  case class WheelEvent(deltaX: Double, deltaY: Double, deltaZ: Double, location: Point) extends CursorEvent

  sealed trait KeyEventType
  case object KeyDown extends KeyEventType
  case object KeyUp extends KeyEventType

  case class KeyboardEvent(eventType: KeyEventType, key: Int, keyModifiers: Int) extends UIEvent

  // stateless ui action
  type Action[M, S] = UIEvent ?=> State[M, S]

  type UIHandler[S] = S => (UIEvent ?=> S)

  // a tool that requires some input state
  type UITool[M, S] = S => Action[M, S]
}
