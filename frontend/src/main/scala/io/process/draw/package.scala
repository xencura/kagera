package io.process

import io.process.geometry._

package object draw {

  def drawNothing[T](e: T): Drawing = Seq.empty

  // Types
  type Drawable[T] = T => Drawing
  type BoundedDrawable[T] = Dimensions => Drawable[T]

  type Drawing = Iterable[DrawInstruction]

  type Tracable[T] = T => Path
  type Path = Iterable[PathSegment]

  type StrokeStyle = String
  type FillStyle = String

  //  implicit def asDrawable[T](tracable[T])

  implicit def drawIterable[T](implicit d: Drawable[T]): Drawable[Iterable[T]] = set => {
    set.toIterable.map(e => d(e)).reduce((a, b) => a ++ b)
  }

  // Drawing Instructions

  sealed trait PathSegment
  case class MoveTo(p: Point) extends PathSegment
  case class LineTo(p: Point) extends PathSegment
  case class Rect(p: Point, width: Double, height: Double) extends PathSegment
  case class Text(text: String, point: Point) extends PathSegment
  case class Arc(centre: Point, radius: Double, startAngle: Double, endAngle: Double) extends PathSegment

  sealed trait DrawInstruction
  case class Fill(style: FillStyle, i: Path) extends DrawInstruction
  case class Stroke(style: StrokeStyle, i: Path) extends DrawInstruction

  case class Clip(rect: Rectangle, drawing: Drawing) extends DrawInstruction
  case class Transform(transform: AffineTransform, drawing: Drawing) extends DrawInstruction

  // DSL
  def transform(t: AffineTransform)(f: => Drawing) = Transform(t, f)
  def rotate(angle: Double)(f: => Drawing) = Transform(AffineTransform.rotation(angle), f)
  def scale(xs: Double, ys: Double)(f: => Drawing) = Transform(AffineTransform.scale(xs, ys), f)
  def translate(xt: Double, yt: Double)(f: => Drawing) = Transform(AffineTransform.translate(xt, yt), f)
  def clip(rect: Rectangle, f: => Drawing) = Clip(rect, f)
  def stroke(style: StrokeStyle, p: Path) = Stroke(style, p)
  def fill(style: FillStyle, p: Path) = Fill(style, p)

  // Implicit conversions

  implicit def asDrawing(instruction: DrawInstruction): Drawing = Seq(instruction)
  implicit def asPath(segment: PathSegment): Path = Seq(segment)

  implicit class PathSegmentAdditions(segment: PathSegment) {
    def ~>(other: PathSegment): Path = Seq(segment, other)
  }

  // Geometry -> Path conversion

  implicit def circleToPath(c: Circle): Path = Arc(c.centre, c.r, 0, 2 * Math.PI)
  implicit def lineSegmentToPath(line: LineSegment): Path = MoveTo(line._1) ~> LineTo(line._2)
}
