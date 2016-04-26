package io.kagera.api.colored

import scala.concurrent.Future

trait Transition {

  type Input
  type Output

  def id: Long
  def label: String

  override def toString = label

  def apply(input: Input): Future[Output]
}
