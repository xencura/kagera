/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.client

import com.raquo.laminar.api.L.Signal
import io.kagera.api.colored.Place
import io.kagera.api.colored.dsl._
import io.kagera.vis.laminar.PetriNetLaminarVisualization
import org.scalajs.dom.html
import scalatags.JsDom.all._

import scala.concurrent.Future
import scala.scalajs.js.annotation.{ JSExport, JSExportTopLevel }
import scala.util.Random

@JSExportTopLevel("FrontendOnly")
object FrontendOnly {
  @JSExport
  def main(container: html.Div) = {
    val graphContainer = div(id := "graph", `class` := "graph").render
    container.appendChild(graphContainer)
    val places = (0 to 2).map(i => Place[Unit](i))
    val transitions = places.zipWithIndex.map { case (_, i) =>
      constantTransition[Unit, Unit, Unit](i, Some(i.toString), constant = ())
    }
    val allEdges = for {
      place <- places
      transition <- transitions
      edge <- Seq(place ~> transition, transition ~> place)
    } yield edge
    val randomEdges = Random.shuffle(allEdges).take(allEdges.size / 3)
    PetriNetLaminarVisualization(
      graphContainer,
      Signal.fromFuture(Future.successful(process(randomEdges: _*))).map(_.get)
    )
  }
}
