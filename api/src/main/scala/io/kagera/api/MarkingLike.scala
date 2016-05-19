package io.kagera.api

/**
 * Type class for marking 'like' semantics.
 */
trait MarkingLike[M, P] {

  /**
   * Returns an empty instance of this marking type.
   *
   * @return
   *   The empty marking.
   */
  def emptyMarking: M

  /**
   * Returns the multiplicity of the marking, that is: A map from place to the nr of tokens in that place
   *
   * @param marking
   *   The marking.
   *
   * @return
   *   The empty marking.
   */
  def multiplicity(marking: M): Marking[P]

  /**
   * Checks if one marking is a sub marking of another
   *
   * @param m
   *   marking
   * @param other
   *   other
   * @return
   *   Whether other is a submarking of m or not
   */
  def isSubMarking(m: M, other: M): Boolean

  /**
   * Consume all tokens in other in from
   *
   * @param from
   *   Tokens
   * @param other
   *   The tokens to consume in from
   * @return
   *   Marking
   */
  def consume(from: M, other: M): M

  /**
   * Removes all tokens in other from from
   *
   * @param from
   *   Tokens
   * @param other
   *   Tokens to remove from from if they exist in from
   * @return
   *   Marking
   */
  def remove(from: M, other: M): M

  /**
   * Produce all tokens in other in from
   *
   * @param into
   *   The marking in which to produce the other tokens
   * @param other
   *   Tokens
   * @return
   *   Marking
   */
  def produce(into: M, other: M): M
}
