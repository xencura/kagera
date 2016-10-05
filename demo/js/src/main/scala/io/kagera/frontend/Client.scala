package io.kagera.frontend

import scalatags.JsDom.all._
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom
import dom.html
import dom.ext.Ajax
import io.kagera.frontend.cytoscape.{ CytoScape, CytoscapeData, Edge, Node }

import scala.scalajs.js
import scalajs.js.annotation.JSExport

@JSExport
object Client extends {

  @JSExport
  def main(container: html.Div) = {

    val graphContainer = div(id := "graph", width := 600, height := 800).render

    //    def refreshIndex() = Ajax.get("/process/dot").foreach { xhr =>
    //      outputBox.innerHTML = ""
    //      outputBox.appendChild(p(xhr.responseText).render)
    //    }

    val n1 = new Node("n1")
    val n2 = new Node("n2")
    val e = new Edge("e1", "n1", "n2")

    container.appendChild(graphContainer)

    val data = new CytoscapeData(graphContainer, js.Array(n1, n2, e))

    val cs = CytoScape(n1)
  }
}
