package io.kagera.frontend

import io.kagera.frontend.cytoscape._
import org.scalajs.dom.html

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{ JSExport, JSExportTopLevel }
import scalatags.JsDom.all._

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
      borderColor := "#0000cc").render

    def getTestProcess() = Api.getProcess("test").foreach { pn ⇒

      val nodes = pn.nodes.map {
        case Left(p)  ⇒ Node(p.label, NodeStyle(width = 30, height = 30, shape = NodeShape.Ellipse, backgroundColor = "blue"))
        case Right(t) ⇒ Node(t.label, NodeStyle(width = 30, height = 30, shape = NodeShape.Rectangle, backgroundColor = "red"))
      }

      val edges = pn.edges.zipWithIndex.map {
        case (e, i) ⇒
          Edge(i.toString, e.source, e.target, EdgeStyle(
            width = 2,
            curveStyle = CurveStyle.Bezier,
            lineColor = "black",
            targetArrow = ArrowStyle(
              color = "black",
              shape = ArrowShape.Triangle)))
      }

      CytoScape.drawGraph(graphContainer, nodes, edges)
    }

    container.appendChild(graphContainer)

    getTestProcess()
  }
}
