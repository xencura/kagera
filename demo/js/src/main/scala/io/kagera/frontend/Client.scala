package io.kagera.frontend

import io.kagera.demo.model.PetriNetModel
import io.kagera.frontend.cytoscape._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Client extends {

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(id := "graph", width := 1024, height := 768).render

    def getTestProcess() = Ajax.get("/process/test").foreach { xhr =>
      val pn = upickle.default.read[PetriNetModel](xhr.responseText)

      val nodes = pn.nodes.map {
        case Left(p) =>
          Node(p.label, NodeStyle(width = 30, height = 30, shape = NodeShape.Ellipse, backgroundColor = "blue"))
        case Right(t) =>
          Node(t.label, NodeStyle(width = 30, height = 30, shape = NodeShape.Rectangle, backgroundColor = "red"))
      }

      val edges = pn.edges.zipWithIndex.map { case (e, i) =>
        Edge(
          i.toString,
          e.source,
          e.target,
          EdgeStyle(
            width = 2,
            curveStyle = CurveStyle.Bezier,
            lineColor = "black",
            targetArrow = ArrowStyle(color = "black", shape = ArrowShape.Triangle)
          )
        )
      }

      CytoScape.drawGraph(graphContainer, nodes, edges)
    }

    container.appendChild(graphContainer)

    getTestProcess()
  }
}
