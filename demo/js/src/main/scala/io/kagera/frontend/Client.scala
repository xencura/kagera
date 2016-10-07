package io.kagera.frontend

import scalatags.JsDom.all._
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom
import dom.html
import dom.ext.Ajax
import io.kagera.frontend.cytoscape._

import scala.scalajs.js
import scalajs.js.annotation.JSExport

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
    }

    CytoScape(data)
  }

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(id := "graph", width := 600, height := 800).render

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
