package io.kagera.api

import scala.PartialFunction._
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.edge._
import scala.language.higherKinds

object ScalaGraph {

  // TODO a bi partite graph is not necessarily directed & weighted
  type BiPartiteGraph[P, T, E[X] <: EdgeLikeIn[X]] = Graph[Either[P, T], E]

  class ScalaGraphPetriNet[P, T](val innerGraph: BiPartiteGraph[P, T, WLDiEdge]) extends PetriNet[P, T] {

    override def inMarking(t: T): Marking[P] = innerGraph.get(t).incoming.map(e => e.source.valueA -> e.weight).toMap
    override def outMarking(t: T): Marking[P] = innerGraph.get(t).outgoing.map(e => e.target.valueA -> e.weight).toMap

    override lazy val places = innerGraph.nodesA().toSet
    override lazy val transitions = innerGraph.nodesB().toSet

    override def nodes() = innerGraph.nodes.map(_.value)
  }

  implicit def placeToNode[P, T](p: P): Either[P, T] = Left(p)
  implicit def transitionToNode[P, T](t: T): Either[P, T] = Right(t)

  implicit class DirectedBiPartiteNodeTAdditions[A, B](val node: BiPartiteGraph[A, B, WLDiEdge]#NodeT) {

    def valueA: A = node.value match {
      case Left(p) => p
      case _ => throw new IllegalStateException(s"node $node is not a place!")
    }

    def valueB: B = node.value match {
      case Right(t) => t
      case _ => throw new IllegalStateException(s"node $node is not a transition!")
    }

    def incomingEdgeB(b: B) = node.incoming.find(_.source.value == Right(b)).map(_.toOuter)
    def outgoingEdgeB(b: B) = node.outgoing.find(_.target.value == Right(b)).map(_.toOuter)

    def incomingEdgeA(a: A) = node.incoming.find(_.source.value == Left(a)).map(_.toOuter)
    def outgoingEdgeA(a: A) = node.outgoing.find(_.target.value == Left(a)).map(_.toOuter)

    def incomingA = node.incoming.map(_.source.valueA)
    def incomingB = node.incoming.map(_.source.valueB)

    def outgoingA = node.outgoing.map(_.target.valueA)
    def outgoingB = node.outgoing.map(_.target.valueB)

    def isNodeA = cond(node.value) { case Left(n) => true }
    def isNodeB = cond(node.value) { case Right(n) => true }
  }

  implicit class DirectedBiPartiteGraphAdditions[A, B](val graph: BiPartiteGraph[A, B, WLDiEdge]) {

    def connectingEdgeAB(from: A, to: B): WLDiEdge[Either[A, B]] =
      graph.get(Left(from)).outgoing.find(_.target.value == Right(to)).get.toOuter

    def connectingEdgeBA(from: B, to: A): WLDiEdge[Either[A, B]] =
      graph.get(Right(from)).outgoing.find(_.target.value == Left(to)).get.toOuter

    def incomingA(b: B): Set[A] = graph.get(b).incomingA

    def outgoingA(b: B): Set[A] = graph.get(b).outgoingA

    def incomingB(a: A): Set[B] = graph.get(a).incomingB

    def outgoingB(b: A): Set[B] = graph.get(b).outgoingB

    def nodesA() = graph.nodes.collect { case n if n.isNodeA => n.valueA }

    def nodesB() = graph.nodes.collect { case n if n.isNodeB => n.valueB }
  }
}
