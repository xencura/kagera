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

package io.kagera.demo.http

import io.kagera.api.colored.{ Place, Transition, _ }
import io.kagera.demo.model

object Util {
  def toModel(pn: ExecutablePetriNet[_]): model.PetriNetModel = {

    val places = pn.nodes.collect { case Left(p) =>
      model.Place(p.id, p.label)
    }

    val transitions = pn.nodes.collect { case Right(t) =>
      model.Transition(t.id, t.label)
    }

    def nodeId(n: Either[Place[_], Transition[_, _, _]]): String = n match {
      case Left(p) => p.label
      case Right(t) => t.label
    }

    val graph = pn.innerGraph

    val edges = graph.edges.map { (e: graph.EdgeT) =>
      model.Edge(nodeId(e.source.value), nodeId(e.target.value), 1)
    }

    model.PetriNetModel(places.toSet, transitions.toSet, edges.toSet)
  }
}
