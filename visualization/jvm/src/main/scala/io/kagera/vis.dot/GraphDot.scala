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

import scala.language.higherKinds
import scalax.collection.Graph
import scalax.collection.GraphPredef.EdgeLikeIn
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._

object GraphDot {

  def generateDot[N, E[+X] <: EdgeLikeIn[X]](graph: Graph[N, E], theme: GraphTheme[N, E]): String = {
    val myRoot =
      DotRootGraph(directed = graph.isDirected, id = None, attrStmts = theme.attrStmts, attrList = theme.rootAttrs)

    def myNodeTransformer(innerNode: Graph[N, E]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
      Some((myRoot, DotNodeStmt(theme.nodeLabelFn(innerNode.value), theme.nodeDotAttrFn(innerNode.value))))
    }

    def myEdgeTransformer(innerEdge: Graph[N, E]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {

      // TODO this is not generic enough
      val source = innerEdge.edge.sources.head.value
      val target = innerEdge.edge.targets.head.value

      Some((myRoot, DotEdgeStmt(theme.nodeLabelFn(source), theme.nodeLabelFn(target), List.empty)))
    }

    graph2DotExport(graph).toDot(
      dotRoot = myRoot,
      edgeTransformer = myEdgeTransformer,
      cNodeTransformer = Some(myNodeTransformer)
    )
  }

  // TODO Generalize this for all types of graphs
  implicit class GraphVisualization[N, E[+X] <: EdgeLikeIn[X]](graph: Graph[N, E]) {

    def toDot(theme: GraphTheme[N, E]): String = generateDot(graph, theme)
  }
}
