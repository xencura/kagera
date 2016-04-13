package io.process.designer.model

import io.process.common.draw._
import io.process.common.draw.ui._
import io.process.common.geometry._

import scalaz.State

case class Layer[S](draw: BoundedDrawable[S], ui: UIHandler[S] = (s: S) => Map.empty[UIEvent, S]) {

  def %(handler: UIHandler[S]): Layer[S] = Layer(draw, handler)

  def &[A](other: Layer[A]): Layer[(S, A)] = Layer[(S, A)](bounds => { case (a, b) =>
    draw(bounds)(a) ++ other.draw(bounds)(b)
  })

  //  def ~>[A](fn: S => Layer[A]): Scene[A :: B]
}
