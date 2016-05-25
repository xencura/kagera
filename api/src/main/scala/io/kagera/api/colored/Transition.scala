package io.kagera.api.colored

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }
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
  val id: Long

  /**
   * A human readable label of this transition.
   *
   * @return
   *   The label.
   */
  val label: String

  /**
   * Flag indicating whether this transition is managed or manually triggered from outside.
   */
  val isManaged: Boolean

  /**
   * The maximum duration this transition may spend doing computation / io.
   */
  val maximumOperationTime: Duration

  /**
   * Creates a valid input type for the transition using the in-adjacent (place, arc, tokens) tuples and optional
   * trigger data.
   *
   * TODO !! This should return a curried function of type:
   *
   * Seq[(Place, WLDiEdge[Node])] => (Seq[Seq[Any]], Option[Any]) => Input
   *
   * This would add a validation phase (calling the first step of the curried function) that can be called during
   * creating the petri net topology.
   *
   * @param inAdjacent
   *   The in-adjacent (place, arc, tokens) tuples.
   * @param data
   *   An optional data field that is given from outside the process.
   * @return
   *   A valid instance of type Input.
   */
  def createInput(
    inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])],
    data: Option[Any],
    context: TransitionContext
  ): Input

  /**
   * Creates a ColoredMarking from the transition output.
   *
   * @param output
   *   An instance of Output
   * @param outAdjacent
   *   The out-adjacent (arc, place) tuples.
   *
   * @return
   *   A valid ColoredMarking
   */
  def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking

  /**
   * Asynchronous function from Input to Output.
   *
   * @param input
   *   input
   *
   * @return
   *   future of output
   */
  def apply(input: Input)(implicit executor: scala.concurrent.ExecutionContext): Future[Output]
}
