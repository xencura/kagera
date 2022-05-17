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

import io.kagera.api._
import io.kagera.api.multiset._

trait ColoredTokenGame extends TokenGame[Place[_], Transition[_, _, _], Marking] {
  this: ColoredPetriNet =>

  override def enabledParameters(m: Marking): Map[Transition[_, _, _], Iterable[Marking]] =
    enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

  def consumableMarkings(marking: Marking)(t: Transition[_, _, _]): Iterable[Marking] = {
    // TODO this is not the most efficient, should break early when consumable tokens < edge weight
    val consumable = inMarking(t).map { case (place, count) =>
      (place, count, consumableTokens(marking, place, t))
    }

    // check if any
    if (consumable.exists { case (place, count, tokens) => tokens.multisetSize < count })
      Seq.empty
    else {
      val consume = consumable.map { case (place, count, tokens) =>
        place -> MultiSet.from(tokens.allElements.take(count))
      }.toMarking

      // TODO lazily compute all permutations instead of only providing the first result
      Seq(consume)
    }
  }

  def consumableTokens[C](marking: Marking, p: Place[C], t: Transition[_, _, _]): MultiSet[C] = {

    val pn = this
    val edge = pn.getEdge(p, t).get

    marking.get(p) match {
      case None => MultiSet.empty
      case Some(tokens) => tokens.filter { case (e, count) => edge.filter(e) }
    }
  }

  // TODO optimize, no need to process all transitions
  override def enabledTransitions(marking: Marking): Set[Transition[_, _, _]] =
    transitions.filter(t => consumableMarkings(marking)(t).nonEmpty)
}
