package io.kagera.client

import io.kagera.api.colored.dsl._
import io.kagera.api.colored.Place
import io.kagera.vis.cytoscape.CytoScapePetriNetVisualization
import org.scalajs.dom.html

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{ JSExport, JSExportTopLevel }
import scalatags.JsDom.svgTags._
import scalatags.JsDom.all._

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
    CytoScapePetriNetVisualization.drawPetriNet(graphContainer, process(randomEdges: _*))
  }
}
