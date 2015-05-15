package io.process.designer.ui

import io.process.geometry.Point

case class MouseEvent(eventType: EventType, button: Int, location: Point)

sealed trait EventType
case object Up extends EventType
case object Down extends EventType
case object Move extends EventType

object MouseTool {
  def nothing[T, S](): MouseTool[T, S] = new MouseTool[T, S] {
    override def onMouseEvent = Map.empty
  }
}

trait MouseTool[M, S] {
  type State = (M, Option[S])

  def onLoseFocus(s: State): State = s
  def onMouseEvent: PartialFunction[(MouseEvent, State), State]
}

trait StateLessMouseTool[M] extends MouseTool[M, None.type] {

  override def onMouseEvent = {
    case (e, (model, None)) if updateModel.isDefinedAt(e, model) =>
      updateModel.lift(e, model) match {
        case Some(updatedModel) => (updatedModel, None)
      }
  }

  def updateModel: PartialFunction[(MouseEvent, M), M]
}
