package io.process.common.geometry

import scalaz._
import scalaz.syntax.std.boolean._

case object AffineTransform {

  private val IDENTITY = AffineTransform(1, 0, 0, 1, 0, 0)
  val identity = IDENTITY

  def scale(p: Point): AffineTransform = scale(p.x, p.y)
  def scale(xs: Double, ys: Double): AffineTransform = AffineTransform(xs, 0, 0, ys, 0, 0)

  def translate(p: Point): AffineTransform = translate(p.x, p.y)
  def translate(xt: Double, yt: Double): AffineTransform = AffineTransform(1, 0, 0, 1, xt, yt)

  def rotation(angle: Double) = {
    val sin = Math.sin(angle)
    val cos = Math.cos(angle)
    AffineTransform(cos, -sin, sin, cos, 0, 0)
  }
}

case class AffineTransform(m: (Double, Double, Double, Double, Double, Double)) extends Function[Point, Point] {

  import PartialFunction._

  def isIdentity: Boolean = cond(m) { case (1, 0, 0, 1, 0, 0) => true }
  def isTranslation: Boolean = cond(m) { case (1, 0, 0, 1, xt, yt) => true }
  def isScale: Boolean = cond(m) { case (xs, 0, 0, ys, 0, 0) => true }

  def rotate(angle: Double) = &(AffineTransform.rotation(angle))
  def scale(p: Point) = &(AffineTransform.scale(p))
  def scale(xs: Double, ys: Double) = &(AffineTransform.scale(xs, ys))
  def translate(xt: Double, yt: Double) = &(AffineTransform.translate(xt, yt))
  def translate(p: Point) = &(AffineTransform.translate(p))

  def &(other: AffineTransform): AffineTransform = {
    val (a1, a2, a3, a4, a5, a6) = m
    val (b1, b2, b3, b4, b5, b6) = other.m

    AffineTransform(
      a1 * b1 + a3 * b2,
      a2 * b1 + a4 * b2,
      a1 * b3 + a3 * b4,
      a2 * b3 + a4 * b4,
      a1 * b5 + a3 * b6 + a5,
      a2 * b5 + a4 * b6 + a6
    )
  }

  lazy val inverse: Option[AffineTransform] = {
    val (a, b, c, d, e, f) = m
    val denominator = (a * d) - (b * c)

    (denominator != 0).option {
      val n = 1 / denominator
      val (w, x, y, z) = (n * d, -n * b, -n * c, n * a)
      val (xt, yt) = (-(w * e + y * f), -(x * e + z * f))

      AffineTransform(w, x, y, z, xt, yt)
    }
  }

  override def apply(p: Point) = Point(m._1 * p.x + m._3 * p.y + m._5, m._2 * p.x + m._4 * p.y + m._6)
}
