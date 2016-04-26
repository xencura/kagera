package io.kagera.api

import io.kagera.api.colored._

object TestProcesses {

  val sum = {

    // places
    val a = Place[Int](id = 1, label = "a")
    val b = Place[Int](id = 2, label = "b")
    val c = Place[Int](id = 3, label = "result")

    // transitions
    val init = TransitionFn(id = 1, label = "init") { () =>
      println("firing transition init")
      (5, 5)
    }

    val sum = TransitionFn(id = 2, label = "sum") { (a: Int, b: Int) =>
      println("firing transition sum")
      a + b
    }

    // process topology
    colored.process(init ~> %(a, b), %(a, b) ~> sum, sum ~> c)
  }
}
