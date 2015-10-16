package io.process
package designer.model

import io.process.common.draw._
import io.process.common.draw.ui._
import io.process.common.geometry._

import scalaz._
import scalaz.std.AllInstances._
import scalaz.syntax.all._

object Foo {
  //
  //  def apply[T](e: T)(implicit drawFn: BoundedDrawable[T] = d => t => Seq.empty) = new Foo[T](
  //    AffineTransform.identity,
  //    drawFn, t => Map.empty)
}

class Foo[T](t: AffineTransform, drawFn: BoundedDrawable[T], eventHandler: T => UIEvent ?=> T)
    extends Transformable[Foo[T]] {

  private def transformEvent(t: AffineTransform): UIEvent ?=> UIEvent = {
    case m @ MouseEvent(_, _, _, _) => m.transform(t)
    case e: UIEvent => e
  }

  def draw: BoundedDrawable[T] = d => e => Transform(t, drawFn(d)(e))
  def apply: T => UIEvent ?=> T = e => eventHandler(e) <<< transformEvent(t)

  def constant[A, B](c: B): A ?=> B = { case e => c }

  def &[B](other: Foo[B]): Foo[(T, B)] = {
    val self = this
    new Foo[(T, B)](
      t,
      d => t => other.draw(d)(t._2) ++ self.draw(d)(t._1),
      t => (self.apply(t._1) &&& constant(t._2)) orElse (constant[UIEvent, T](t._1) &&& other.apply(t._2))
    )
  }

  //  def ~>[B](fn: T => Foo[B]) = {
  //    val self = this
  //    new Foo[T](t,
  //      d => t => self.draw(t) ++ fn(t).draw
  //    )
  //  }

  //  def %(fn: T => UIEvent ?=> Foo[T]): Foo[T]

  def emap(fn: T => UIEvent ?=> T): Foo[T] = new Foo[T](t, drawFn, fn)

  def #:(drawing: Drawing) = new Foo[T](t, d => e => drawing ++ drawFn(d)(e), eventHandler)
  def :#(drawing: Drawing) = new Foo[T](t, d => e => drawFn(d)(e) ++ drawing, eventHandler)

  //  def ~>[B](fn: T => Foo[B]): Foo[(T, B)]
  override def transform(other: AffineTransform) = new Foo[T](t & other, drawFn, eventHandler)
}
