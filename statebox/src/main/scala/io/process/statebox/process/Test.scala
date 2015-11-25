package io.process.statebox.process

import io.process.statebox.process.dsl._

object Test extends App {

  val a = Place[Int]("a")
  val b = Place[Int]("b")
  val c = Place[Int]("result")

  def init() = (5, 5)
  def sum(a: Int, b: Int) = a + b

  val sumT = toTransition2("sum", sum)
  val initT = toTransition0("init", init)

  val p = process(initT ~> %(a, b), %(a, b) ~> sumT, sumT ~> c)

  println(p.toDot)

  println(p.enabledTransitions(Map(a -> 1, b -> 2)))
}
