package io.process.statebox.process.colored

object TestProcesses {

  val sum = {

    // places
    val a = Place[Int](id = 1, label = "a")
    val b = Place[Int](id = 2, label = "b")
    val c = Place[Int](id = 3, label = "result")

    // transitions
    val init = TFn(id = 1, label = "init") { () =>
      (5, 5)
    }

    val sum = TFn(id = 2, label = "sum") { (a: Int, b: Int) =>
      a + b
    }

    // process topology
    process(init ~> %(a, b), %(a, b) ~> sum, sum ~> c)
  }
}
