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

package io.kagera.demo

import upickle.default._

package object model {

  case class Place(id: Long, label: String)
  object Place {
    implicit val rw: ReadWriter[Place] = macroRW[Place]
  }

  case class Transition(id: Long, label: String, code: String = "", input: Option[String] = None)
  object Transition {
    implicit val rw: ReadWriter[Transition] = macroRW[Transition]
  }

  case class Edge(source: String, target: String, weigth: Int = 1)
  object Edge {
    implicit val rw: ReadWriter[Edge] = macroRW[Edge]
  }

  case class PetriNetModel(places: Set[Place], transitions: Set[Transition], edges: Set[Edge]) {

    def nodes: Set[Either[Place, Transition]] = places.map(p => Left(p)) ++ transitions.map(t => Right(t))
  }
  object PetriNetModel {
    implicit val rw: ReadWriter[PetriNetModel] = macroRW[PetriNetModel]
  }

  case class ProcessState(marking: Map[Long, Int], data: Map[String, String])
}
