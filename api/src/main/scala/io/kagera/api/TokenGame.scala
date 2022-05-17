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

package io.kagera.api

object TokenGame {

  // given a process and current marking picks the next transition and marking to fire
  type Step[P, T, M] = (TokenGame[P, T, M], M) => Option[(M, T)]

  def stepFirst[P, T, M]: Step[P, T, M] = (process, marking) => {
    process.enabledParameters(marking).headOption.map { case (t, enabledMarkings) => (enabledMarkings.head, t) }
  }

  def stepRandom[P, T, M]: Step[P, T, M] = (process, marking) => {
    import scala.util.Random

    val params = process.enabledParameters(marking)

    params.nonEmpty.option {
      val n = Random.nextInt(Math.min(10, params.size))
      val (t, enabledMarkings) = LazyList.continually(params.to(LazyList)).flatten.apply(n)
      (enabledMarkings.head, t)
    }
  }
}

/**
 * Interface for deciding which (transition, marking) parameters are 'enabled'
 *
 * @tparam P
 *   Place
 * @tparam T
 *   Transition
 * @tparam M
 *   Marking The type of Marking in the PetriNet
 */
trait TokenGame[P, T, M] {

  this: PetriNet[P, T] =>

  def enabledParameters(marking: M): Map[T, Iterable[M]] = {
    // inefficient, fix
    enabledTransitions(marking).view.map(t => t -> consumableMarkings(marking)(t)).toMap
  }

  def consumableMarkings(marking: M)(t: T): Iterable[M]

  /**
   * Checks whether a transition is 'enabled' in a marking.
   *
   * @param marking
   *   The marking.
   * @param t
   *   The transition.
   * @return
   */
  def isEnabled(marking: M)(t: T): Boolean = consumableMarkings(marking)(t).nonEmpty

  /**
   * Returns all enabled transitions for a marking.
   *
   * @param marking
   *   marking
   * @return
   */
  def enabledTransitions(marking: M): Set[T]
}
