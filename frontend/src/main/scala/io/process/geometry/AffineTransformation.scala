package io.process.geometry

case object AffineTransformation {

  private val IDENTITY = AffineTransformation(1, 0, 0, 1, 0, 0)
  val identity = IDENTITY
  def scale(xs: Double, ys: Double) = AffineTransformation(xs, 0, 0, ys, 0, 0)
  def translate(xt: Double, yt: Double) = AffineTransformation(1, 0, 0, 1, xt, yt)
}

case class AffineTransformation(m: (Double, Double, Double, Double, Double, Double)) extends Function[Point, Point] {

  import PartialFunction._
  import scalaz.syntax.std.boolean._

  def isIdentity: Boolean = cond(m) { case (1, 0, 0, 1, 0, 0) => true }
  def isTranslation: Boolean = cond(m) { case (1, 0, 0, 1, xt, yt) => true }
  def isScale: Boolean = cond(m) { case (xs, 0, 0, ys, 0, 0) => true }

  lazy val inverse: Option[AffineTransformation] = {
    val (a, b, c, d, e, f) = m
    val denominator = (a * d - b * c)

    (denominator != 0).option {
      val n = 1 / denominator
      // TODO this calculation is not correct, fix
      val (w, x, y, z) = (n * d, -n * b, -n * c, n * a)
      AffineTransformation(w, x, y, z, e, f)
    }
  }

  override def apply(p: Point) = Point(m._1 * p.x + m._3 * p.y + m._5, m._2 * p.x + m._4 * p.y + m._6)
}
