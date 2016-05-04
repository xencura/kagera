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
   * @param other
   * @return
   */
  def isSubMarking(m: M, other: M): Boolean

  def consume(from: M, other: M): M

  def produce(into: M, other: M): M
}
