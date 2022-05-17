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

package io.kagera.vis.cytoscape

import io.kagera.api.colored.ColoredPetriNet
import org.scalajs.dom.html.Div

object CytoScapePetriNetVisualization {

  def drawPetriNet[P, T](graphContainer: Div, pn: ColoredPetriNet) = {
    val nodes = pn.nodes.map {
      case Left(p) =>
        Node(p.toString, NodeStyle(width = 30, height = 30, shape = NodeShape.Ellipse, backgroundColor = "blue"))
      case Right(t) =>
        Node(t.toString, NodeStyle(width = 30, height = 30, shape = NodeShape.Rectangle, backgroundColor = "red"))
    }.toSet

    val edges = pn.edges.zipWithIndex.map { case (e, i) =>
      Edge(
        i.toString,
        e.source.toString,
        e.target.toString,
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
}
