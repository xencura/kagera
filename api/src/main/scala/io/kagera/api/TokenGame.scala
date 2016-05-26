package io.kagera.api

object TokenGame {

  // given a process and current marking picks the next transition and marking to fire
  type Step[P, T, M] = (TokenGame[P, T, M], M) => Option[(M, T)]

  def stepFirst[P, T, M]: Step[P, T, M] = (process, marking) => {
    process.enabledParameters(marking).headOption.map { case (t, enabledMarkings) => (enabledMarkings.head, t) }
  }

  def stepRandom[P, T, M]: Step[P, T, M] = (process, marking) => {
    import scalaz.syntax.std.boolean._
    import scala.util.Random

    val params = process.enabledParameters(marking)

    params.nonEmpty.option {
      val n = Random.nextInt(Math.min(10, params.size))
      val (t, enabledMarkings) = Stream.continually(params.toStream).flatten.apply(n)
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
 *   Marking
 */
trait TokenGame[P, T, M] extends PetriNet[P, T] {

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
