package io.kagera.frontend

import scalatags.JsDom.all._
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom
import dom.html
import dom.ext.Ajax
import io.kagera.demo.model.PetriNetModel
import io.kagera.frontend.cytoscape._

import scala.scalajs.js
import scalajs.js.annotation.{ JSExport, JSName }

@JSExport
object Client extends {

  val defaultNodeStyle = NodeStyle(20, 20, NodeShape.Ellipse)
  val defaultEdgeStyle = EdgeStyle(3, CurveStyle.Haystack, "red")

  def drawGraph(graphContainer: html.Element, nodes: Set[Node], edges: Set[Edge]) = {

    val data = new js.Object {

      val nodeElements: Seq[js.Object] = nodes.map { n =>
        val style = n.style
        new js.Object {
          val data = new js.Object {
            val id = n.id
            val shape = style.shape.name
            val width = style.width
            val height = style.height
            val backgroundColor = style.backgroundColor
          }
        }
      }.toSeq

      val edgeElements: Seq[js.Object] = edges.map { e =>
        val style = e.edgeStyle
        new js.Object {
          val data = new js.Object {
            val id = e.id
            val source = e.source
            val target = e.target
            val lineWidth = style.width
            val lineColor = style.lineColor

            val targetArrowShape = style.targetArrow.shape.name
            val targetArrowColor = style.targetArrow.color
            val targetArrowFill = style.targetArrow.fill

            val sourceArrowShape = style.sourceArrow.shape.name
            val sourceArrowColor = style.sourceArrow.color
            val sourceArrowFill = style.sourceArrow.fill

            val curveStyle = style.curveStyle.name
          }
        }
      }.toSeq

      val edgeStyleSelector = new js.Object {
        val selector = "edge"

        val style = new js.Object {

          @JSName("curve-style")
          val curveStyle = "data(curveStyle)"

          @JSName("target-arrow-shape")
          val targetArrowShape = "data(targetArrowShape)"

          @JSName("target-arrow-color")
          val targetArrowColor = "data(targetArrowColor)"

          @JSName("target-arrow-fill")
          val targetArrowFill = "data(targetArrowFill)"

          @JSName("source-arrow-shape")
          val sourceArrowShape = "data(sourceArrowShape)"

          @JSName("source-arrow-color")
          val sourceArrowColor = "data(sourceArrowColor)"

          @JSName("source-arrow-fill")
          val sourceArrowFill = "data(sourceArrowFill)"

          val width = "data(lineWidth)"

          @JSName("line-color")
          val lineColor = "data(lineColor)"
        }
      }

      val nodeStyleSelector = new js.Object {
        val selector = "node"
        val style = new js.Object {
          val width = "data(width)"
          val height = "data(height)"
          val shape = "data(shape)"
          //          @JSName("background-color")
          //          val backgroundColor = "data(backgroundColor)"
        }
      }

      val container = graphContainer

      val elements: js.Array[js.Object] = js.Array((nodeElements ++ edgeElements): _*)

      val style: js.Array[js.Object] = js.Array(edgeStyleSelector, nodeStyleSelector)

    }

    CytoScape(data)
  }

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(id := "graph", width := 600, height := 800).render

    def getTestProcess() = Ajax.get("/process/test").foreach { xhr =>
      val pn = upickle.default.read[PetriNetModel](xhr.responseText)

      val nodes = pn.nodes.map {
        case Left(p) => Node(p.label, NodeStyle(width = 20, height = 20, shape = NodeShape.Ellipse))
        case Right(t) => Node(t.label, NodeStyle(width = 20, height = 20, shape = NodeShape.Rectangle))
      }

      val edges = pn.edges.zipWithIndex.map { case (e, i) =>
        Edge(
          i.toString,
          e.source,
          e.target,
          EdgeStyle(
            width = 3,
            curveStyle = CurveStyle.Bezier,
            lineColor = "black",
            targetArrow = ArrowStyle(color = "red", shape = ArrowShape.Triangle)
          )
        )
      }

      drawGraph(graphContainer, nodes, edges)
    }

    container.appendChild(graphContainer)

    getTestProcess()
  }
}
