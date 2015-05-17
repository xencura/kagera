package io.process.designer.ui

import io.process.geometry.Point

sealed trait EventType
case object Up extends EventType
case object Down extends EventType
case object Move extends EventType

case class MouseEvent(eventType: EventType, button: Int, location: Point)
