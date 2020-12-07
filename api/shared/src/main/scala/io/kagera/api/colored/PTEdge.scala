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
   * Filter predicate function, if false the out-adjacent transition may not consume the token.
   *
   * TODO
   *
   * A predicate can not communicate a reason for failure.
   *
   * Perhaps better would be:
   *
   * T => Option(String)
   *
   * in which case you can provide a reason message for not being able to consume a token.
   *
   * @return A predicate function from token -> boolean, indicating whether the token may be consumed (true) or not (false)
   */
  val filter: T => Boolean
}
