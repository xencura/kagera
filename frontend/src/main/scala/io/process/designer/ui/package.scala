package io.process.designer

import io.process.geometry.Point

import scalaz._

package object ui {

  sealed trait UIEvent

  case object FocusLost extends UIEvent

  sealed trait MouseEventType
  case object MouseUp extends MouseEventType
  case object MouseDown extends MouseEventType
  case object MouseMove extends MouseEventType

  case class MouseEvent(eventType: MouseEventType, button: Int, location: Point, modifiers: Int) extends UIEvent

  sealed trait KeyEventType
  case object KeyDown extends KeyEventType
  case object KeyUp extends KeyEventType

  case class KeyboardEvent(eventType: KeyEventType, key: Int, modifiers: Int) extends UIEvent

  def actionFn[M, S](pf: PartialFunction[UIEvent, M => (M, S)]): Action[M, S] = pf.andThen(fn => State { s: M => fn(s) })

  // stateless ui action
  type Action[M, S] = PartialFunction[UIEvent, State[M, S]]

  // a tool that requires some input state
  type UITool[M, S] = S => Action[M, S]

  implicit class ActionFunctions[M, S](action: Action[M, S]) {

    def |(other: Action[M, S]) = action.orElse(other)
  }

  implicit class ToolFunctions[M, S](tool: UITool[M, S]) {
    def |(other: UITool[M, S]): UITool[M, S] = { state =>
      val fn1 = tool(state)
      val fn2 = other(state)
      fn1
    }
  }
}
