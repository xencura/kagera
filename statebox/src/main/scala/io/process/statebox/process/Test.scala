package io.process.statebox.process

import akka.actor.Props
import akka.util.Timeout
import io.process.statebox.ServicesImpl
import io.process.statebox.process.PetriNetDebugging.Step
import io.process.statebox.process.dsl._

import scala.concurrent.Await

object Test extends App with ServicesImpl {

  val a = Place[Int]("a")
  val b = Place[Int]("b")
  val c = Place[Int]("result")

  def init() = (5, 5)
  def sum(a: Int, b: Int) = a + b

  val sumT = toTransition2("sum", sum)
  val initT = toTransition0("init", init)

  val simpleProcess = process(initT ~> %(a, b), %(a, b) ~> sumT, sumT ~> c)

  val actor = system.actorOf(Props(new PetriNetActor(simpleProcess)))

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  actor ? Step

  Await.result(actor ? Step, timeout.duration)
}
