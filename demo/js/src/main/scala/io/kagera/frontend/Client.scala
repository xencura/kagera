package io.kagera.frontend

import io.kagera.frontend.cytoscape._
import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.{ JSExport, JSName }
import scalatags.JsDom.all._

@JSExport
object Client extends {

  def drawGraph(graphContainer: html.Element, nodes: Set[Node], edges: Set[Edge]) = {
    val data = new js.Object {

      val nodeElements: Seq[js.Object] = nodes
        .map(n =>
          new js.Object {
            val data = new js.Object {
              val id = n.id
            }
          }
        )
        .toSeq

      val edgeElements: Seq[js.Object] = edges
        .map(e =>
          new js.Object {
            val data = new js.Object {
              val id = e.id
              val source = e.source
              val target = e.target
            }
          }
        )
        .toSeq

      val container = graphContainer
      val elements: js.Array[js.Object] = js.Array((nodeElements ++ edgeElements): _*)
      val style: js.Array[js.Object] = js.Array(new js.Object {
        val selector = "edge"
        val style = new js.Object {
          val width = 2
          @JSName("line-color")
          val lineColor = "#00f"
          @JSName("target-arrow-color")
          val targetArrowColor = "#444"
          @JSName("target-arrow-shape")
          val targetArrowShape = "triangle"
        }
      });
      val layout = new js.Object {
        val name = "grid"
        val rows = 1
      }
    }

    CytoScape(data)
  }

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(id := "graph", height := 800, width := 600).render

    //    def refreshIndex() = Ajax.get("/process/dot").foreach { xhr =>
    //      outputBox.innerHTML = ""
    //      outputBox.appendChild(p(xhr.responseText).render)
    //    }

    container.appendChild(graphContainer)

    val nodes = Set(Node("n1"), Node("n2"))
    val edges = Set(Edge("e", "n1", "n2"))

    drawGraph(graphContainer, nodes, edges)
  }
}
