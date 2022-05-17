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

package io.kagera.dot

import io.kagera.api.multiset.MultiSet

import scala.language.higherKinds
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._

object PetriNetDotVisualization {

  def labelFn[P, T]: Either[P, T] => String = node =>
    node match {
      case Left(a) => a.toString
      case Right(b) => b.toString
    }

  def petriNetTheme[P, T]: GraphTheme[Either[P, T], WLDiEdge] = new GraphTheme[Either[P, T], WLDiEdge] {

    override def nodeLabelFn = labelFn

    override def nodeDotAttrFn = node =>
      node match {
        case Left(nodeA) => List(DotAttr("shape", "circle"))
        case Right(nodeB) => List(DotAttr("shape", "square"))
      }
  }

  def markedPetriNetTheme[P, T](marking: MultiSet[P]): GraphTheme[Either[P, T], WLDiEdge] =
    new GraphTheme[Either[P, T], WLDiEdge] {

      override def nodeLabelFn = labelFn
      override def nodeDotAttrFn = node =>
        node match {
          case Left(nodeA) =>
            marking.get(nodeA) match {
              case Some(n) if n > 0 =>
                List(
                  DotAttr("shape", "doublecircle"),
                  DotAttr("color", "darkorange"),
                  DotAttr("style", "filled"),
                  DotAttr("fillcolor", "darkorange"),
                  DotAttr("penwidth", 2)
                )
              case _ => List(DotAttr("shape", "circle"), DotAttr("color", "darkorange"), DotAttr("penwidth", 2))
            }
          case Right(nodeB) => List(DotAttr("shape", "square"))
        }
    }

  // TODO Generalize this for all types of graphs
  implicit class PetriNetVisualization[P, T](graph: Graph[Either[P, T], WLDiEdge]) {

    def toDot(): String = toDot(petriNetTheme[P, T])

    def toDot(theme: GraphTheme[Either[P, T], WLDiEdge]): String = GraphDot.generateDot(graph, theme)
  }
}
