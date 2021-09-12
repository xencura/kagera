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
