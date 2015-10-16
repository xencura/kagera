package io.process.common.geometry

trait Transformable[T] {

  def translate(xt: Double, yt: Double): T = transform(AffineTransform.translate(xt, yt))
  def rotate(angle: Double): T = transform(AffineTransform.rotation(angle))
  def scale(xs: Double, ys: Double): T = transform(AffineTransform.scale(xs, ys))
  def transform(t: AffineTransform): T
}
