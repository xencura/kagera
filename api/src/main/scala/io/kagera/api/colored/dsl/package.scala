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

package io.kagera.api.colored

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api._
import io.kagera.api.colored.transitions.{ AbstractTransition, IdentityTransition }
import io.kagera.api.multiset._
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

import scala.concurrent.duration.Duration

/**
 * TODO:
 *
 * This is not much of a DSL yet.
 *
 * Insight:
 *
 * Since each transition is different in what kind of in/out places & edges it can take we should probably not create a
 * general connectivity DSL based on the base trait Transition.
 */
package object dsl {

  implicit class TransitionDSL[Input, Output, State](t: Transition[Input, Output, State]) {
    def ~>(p: Place[_], weight: Long = 1): Arc = arc(t, p, weight)
  }

  implicit class PlaceDSL[C](p: Place[C]) {
    def ~>(t: Transition[_, _, _], weight: Long = 1, filter: C => Boolean = token => true): Arc =
      arc(p, t, weight, filter)
  }

  def arc(t: Transition[_, _, _], p: Place[_], weight: Long): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight.toDouble, "")

  def arc[C](p: Place[C], t: Transition[_, _, _], weight: Long = 1L, filter: C => Boolean = (token: C) => true): Arc = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node, PTEdge[C]](Left(p), Right(t))(weight.toDouble, innerEdge)
  }

  def constantTransition[I, O, S](
    id: Long,
    label: Option[String] = None,
    automated: Boolean = false,
    constant: O
  ): Transition[I, O, S] =
    new AbstractTransition[I, O, S](id, label.getOrElse(s"t$id"), automated, Duration.Undefined)
      with IdentityTransition[I, O, S] {

      override val toString: String = label

      override def apply[F[_]](inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
        sync: Sync[F],
        applicativeError: ApplicativeError[F, Throwable]
      ) =
        (marking, state, input) =>
          sync.delay {
            val produced = outAdjacent.map { case (place, weight) =>
              place -> Map(constant -> weight)
            }.toMarking

            (produced, constant)
          }
    }

  def nullTransition[S](id: Long, label: Option[String] = None, automated: Boolean = false): Transition[Unit, Unit, S] =
    constantTransition[Unit, Unit, S](id, label, automated, ())

  def process[S](params: Arc*): ExecutablePetriNet[S] = {
    val petriNet = new ScalaGraphPetriNet(Graph(params: _*)) with ColoredTokenGame

    requireUniqueElements(petriNet.places.toSeq.map(_.id), "Place identifier")
    requireUniqueElements(petriNet.transitions.toSeq.map(_.id), "Transition identifier")

    petriNet
  }
}
