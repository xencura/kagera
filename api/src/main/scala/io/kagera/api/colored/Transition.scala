package io.kagera.api.colored

import scala.concurrent.Future
import scala.reflect.runtime.{ universe => ru }
import scalax.collection.edge.WLDiEdge

/**
 * A transition in a colored petri net.
 */
trait Transition {

  /**
   * The input type of this transition.
   */
  type Input

  /**
   * The output type of this transition.
   */
  type Output

  /**
   * The unique identifier of this transition.
   *
   * @return
   *   The unique identifier.
   */
  def id: Long

  /**
   * A human readable label of this transition.
   *
   * @return
   *   The label.
   */
  def label: String

  /**
   * Creates a valid input type for the transition using the in-adjacent (place, arc, tokens) tuples and optional
   * trigger data.
   *
   * @param input
   *   The in-adjacent (place, arc, tokens) tuples.
   * @param data
   *   An optional data field that is given from outside the process.
   * @return
   *   A valid instance of type Input.
   */
  def createInput(input: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input

  def createOutput(output: Output, places: Seq[(WLDiEdge[Node], Place)]): ColoredMarking

  def apply(input: Input): Future[Output]
}
