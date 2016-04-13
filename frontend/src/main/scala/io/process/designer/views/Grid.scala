package io.process.designer.views

import io.process.designer.model.{ Layer, Layer$ }
import io.process.common.draw._
import io.process.common.geometry._

object Grid {

  case class GridProperties(gridX: Int, gridY: Int, strokeStyle: StrokeStyle) {
    def snapToIS(point: Point): Point = Point(roundToFactor(point.x, gridX), roundToFactor(point.y, gridY))
    def roundToFactor(n: Double, factor: Long): Long = math.round(n / factor) * factor
  }

  implicit val drawGrid: BoundedDrawable[GridProperties] = dimensions =>
    props => {

      val (gridX, gridY) = (props.gridX, props.gridY)
      val (w, h) = (dimensions.width, dimensions.height)

      val xlines = (1 until (w / gridX)).map(gridX *).map { x => MoveTo(x, 0) ~> LineTo(x, h) }.flatten
      val ylines = (1 until (h / gridY)).map(gridY *).map { y => MoveTo(0, y) ~> LineTo(w, y) }.flatten

      translate(0.5, 0.5) {
        stroke(props.strokeStyle, xlines ++ ylines)
      }
    }
}
