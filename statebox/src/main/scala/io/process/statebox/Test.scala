package io.process.statebox

import scalax.collection.immutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.edge.Implicits._

object Test extends App {

  val (t1, t2) = (1, 2)

  val p1 = (Int, "start")
  val p2 = (String, "end")

  def t1(i: Int): String = i.toString
  def t2(i: String): Int = i.length

  val topology = Graph(t1 ~> p2 % 1, p2 ~> t2 % 1)

  println(g.nodes)
}
