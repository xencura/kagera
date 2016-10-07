package io.kagera.frontend.cytoscape

import scala.scalajs.js
import scala.scalajs.js.annotation.{ JSName, ScalaJSDefined }

@ScalaJSDefined
class EdgeStyle(
  val width: String,
  val curveStyle: String = "haystack",
  @JSName("line-color") val lineColor: String = "black"
) extends js.Object {}
