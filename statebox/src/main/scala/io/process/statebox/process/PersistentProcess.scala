//package io.process.statebox.process
//
//import akka.persistence.PersistentActor
//import akka.shapeless._
//
//trait Step[I, O, Event] {
//
//  def process: PartialFunction[Any, Unit]
//
//  def complete(out: Event, persist: Event => Out)
//}
//
//class Process {
//
//  sealed trait Event
//
//  def t1: Step[Int, Int, Event] = ???
//  def t2: Step[String, Long, Event] = ???
//
//  def foo[In1, In2, Out1, Out2, E](s1: Step[In1, Out1, E],
//    s2: Step[In2, Out2, E]): Step[In1 :: In2 :: HNil, Out1 :: Out2 :: HNil, E]
//
//  val f = t1 | t2
//
//  def concat[A, B](a: A, b: B): A :: B :: HNil
//
//  //  def process = t1 ~> (t2 foo t3 | t4) ~> t5
//}
//
//trait PersistentProcess extends PersistentActor {
//
//  val activeSteps: Set[Step[_, _]]
//}
//
//object Foo extends App {
//
//  trait Magnet[A, B] {
//    type Result
//    def split(result: Result): (A, B)
//    def join(i: A, o: B): Result
//  }
//
//  class Tuple1Magnet[A, B] extends Magnet[Tuple1[A], Tuple1[B]] {
//    override type Result = (A, B)
//    override def split(t2: Result) = (Tuple1(t2._1), Tuple1(t2._2))
//    override def join(i: Tuple1[A], o: Tuple1[B]): Result = (i._1, o._1)
//  }
//
//  implicit def magnet1[A, B] = new Tuple1Magnet[A, B]()
//
//  implicit class MagnetJoin[I1, O1](fn1: I1 => O1) {
//
//    def |[I2, O2](fn2: I2 => O2)(implicit m1: Magnet[I1, I2], m2: Magnet[O1, O2]): m1.Result => m2.Result = {
//      input =>
//        val (a, b) = m1.split(input)
//        m2.join(fn1.apply(a), fn2.apply(b))
//    }
//  }
//
//  def a: (Int) => Tuple1[Int] = i => Tuple1(i * 2)
//  def b: (String) => Tuple1[Int] = s => Tuple1(s.length)
//
//  def c = a | b
//
//  c.apply((1, "foo"))
//
//}
