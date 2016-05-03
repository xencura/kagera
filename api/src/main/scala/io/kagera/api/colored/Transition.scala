package io.kagera.api.colored

import scala.concurrent.Future
import scala.reflect.runtime.{ universe => ru }
import scalax.collection.edge.WLDiEdge

trait Transition {

  type Input
  type Output

  def id: Long
  def label: String

  override def toString = label

  def createInput(input: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input
  def createOutput(output: Output, places: Seq[(WLDiEdge[Node], Place)]): ColoredMarking

  def apply(input: Input): Future[Output]
}
