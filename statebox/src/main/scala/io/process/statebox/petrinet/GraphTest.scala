package io.process.statebox.petrinet

import scalax.collection.GraphPredef._
import scalax.collection.edge.Implicits._
import scalax.collection.edge.WDiEdge
import scalax.collection.immutable.Graph

class BiWDiEdge[C, A <: C, B <: C](from: A, to: B, weight: Int = 1) extends WDiEdge[Any]((from, to), weight)

object GraphTest extends App {

  val (t1, t2) = (1, 2)

  val p1 = (classOf[Int], "start")
  val p2 = (classOf[String], "end")

  def t1(i: Int): String = i.toString
  def t2(i: String): Int = i.length

  //  def partitionedGraph[A, B](p1: Seq[BiWDiEdge[A, B]], p2: Seq[BiWDiEdge[B, A]]) = Graph(p1 ++ p2: _*)

  //  val bipgraph = partitionedGraph[Int, String](
  //    Seq(new BiWDiEdge(1, "1")),
  //    Seq(new BiWDiEdge("1", 2))
  //  )
  //
  //  bipgraph.nodes.filter(p => p.value.isInstanceOf[B])

  //  println(bipgraph)

  val topology = Graph(t1 ~> p2 % 1, p2 ~> t2 % 1)

  println(topology)
}
