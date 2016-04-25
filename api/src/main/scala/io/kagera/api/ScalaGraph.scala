package io.kagera.api

import scala.PartialFunction._
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

object ScalaGraph {

  // TODO a bi partite graph is not necessarily directed & weighted
  type BiPartiteGraph[P, T] = Graph[Either[P, T], WDiEdge]

  class ScalaGraphWrapper[P, T](val innerGraph: BiPartiteGraph[P, T]) extends PetriNet[P, T] {

    override def inMarking(t: T): Marking[P] = innerGraph.get(t).incoming.map(e => e.source.valueA -> e.weight).toMap
    override def outMarking(t: T): Marking[P] = innerGraph.get(t).outgoing.map(e => e.target.valueA -> e.weight).toMap

    override lazy val places = innerGraph.nodesA().toSet
    override lazy val transitions = innerGraph.nodesB().toSet

    override def nodes() = innerGraph.nodes.map(_.value)
  }

  implicit def placeToNode[P, T](p: P): Either[P, T] = Left(p)
  implicit def transitionToNode[P, T](t: T): Either[P, T] = Right(t)

  implicit class BiPartiteNodeTAdditions[A, B](val node: BiPartiteGraph[A, B]#NodeT) {

    def valueA: A = node.value match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }

    def valueB: B = node.value match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a transition!")
    }

    def incomingA = node.incoming.map(_.source.valueA)
    def incomingB = node.incoming.map(_.source.valueB)

    def outgoingA = node.outgoing.map(_.target.valueA)
    def outgoingB = node.outgoing.map(_.target.valueB)

    def isNodeA = cond(node.value) { case Left(n) => true }
    def isNodeB = cond(node.value) { case Right(n) => true }
  }

  implicit class BiPartiteGraphAdditions[A, B](val process: BiPartiteGraph[A, B]) {

    def incomingA(b: B): Set[A] = process.get(b).incomingA

    def outgoingA(b: B): Set[A] = process.get(b).outgoingA

    def incomingB(a: A): Set[B] = process.get(a).incomingB

    def outgoingB(b: A): Set[B] = process.get(b).outgoingB

    def nodesA() = process.nodes.collect { case n if n.isNodeA => n.valueA }

    def nodesB() = process.nodes.collect { case n if n.isNodeB => n.valueB }
  }
}
