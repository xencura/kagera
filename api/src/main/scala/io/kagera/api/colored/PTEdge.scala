package io.kagera.api.colored

/**
 * An edge from a place to a transition.
 */
trait PTEdge[T] {

  /**
   * The weight of the edge.
   */
  val weight: Long

  /**
   * Filter function, if true to out-adjacent transition may not consume the token.
   * @param token
   * @return
   */
  val filter: T => Boolean
}
