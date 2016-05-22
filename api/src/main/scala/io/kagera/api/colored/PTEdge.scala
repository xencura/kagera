package io.kagera.api.colored

/**
 * An edge from a place to a transition.
 */
trait PTEdge {

  /**
   * The weight of the edge.
   */
  val weight: Long

  /**
   * Filter function, if true to out-adjacent transition may not consume the token.
   *
   * @param place
   * @param token
   * @return
   */
  val filter: Place => Place#Color => Boolean
}
