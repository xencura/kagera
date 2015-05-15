package io.process.statebox

import scalax.collection.edge.WDiEdge
import scalax.collection.immutable.Graph

class Arc[A, B](nodes: (A, B), weight: Long = 1) extends WDiEdge[A](nodes, weight)

object PetriNet {

  def apply[A, B](pt: Seq[Arc[A, B]], tp: Seq[Arc[B, A]]): PetriNet[A, B] = ???
}

trait PetriNet[P, T] extends Graph[Any, Arc] {

//  def places

//  def transitions

}
