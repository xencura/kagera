package io.kagera.client

import io.kagera.api.colored.Place
import io.kagera.api.colored.dsl._
import io.kagera.vis.cytoscape._
import org.scalajs.dom.html
import scalatags.JsDom.all._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{ JSExport, JSExportTopLevel }

@JSExportTopLevel("Client")
object Client extends {

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(
      id := "graph",
      `class` := "graph",
      width := 1024,
      height := 512,
      backgroundColor := "#fafaff",
      borderWidth := "2px",
      borderStyle := "solid",
      borderColor := "#0000cc"
    ).render

    def getTestProcess() =
      Api.getProcess("test").foreach { pn =>
        val places = pn.nodes.collect { case Left(p) => p.id.toString -> Place[String](p.id, Some(p.label)) }.toMap
        val transitions = pn.nodes.collect { case Right(t) =>
          t.id.toString -> constantTransition[String, String, String](t.id, Some(t.label), constant = t.code)
        }.toMap
        val edges = pn.edges.map {
          case e if places.contains(e.source) => places(e.source) ~> transitions(e.target)
          case e if transitions.contains(e.source) => transitions(e.source) ~> places(e.target)
        }
        CytoScapePetriNetVisualization.drawPetriNet(graphContainer, process(edges.toSeq: _*))
      }

    container.appendChild(graphContainer)

    getTestProcess()
  }
}
