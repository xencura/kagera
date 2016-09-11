# Kagera. A Discrete, colored Petri Net DSL & Executor.

Colored Petri net example:

```
val sum = {

    // places
    val a = Place[Int](id = 1, label = "a")
    val b = Place[Int](id = 2, label = "b")
    val c = Place[Int](id = 3, label = "result")

    // transitions
    val init = Transition(id = 1, label = "init") {
      () => (5, 5)
    }

    val sum = Transition(id = 2, label = "sum") {
      (a: Int, b: Int) => a + b
    }

    // process topology
    process(
      init    ~> %(a, b),
      %(a, b) ~> sum,
      sum     ~> c)
  }

```

Short term plans:
* Http API (using akka http)
* Clustering & Sharding
* Other execution models, such as:
  * Splitting processes over multiple actors
  * Streams (e.g. https://github.com/functional-streams-for-scala/fs2) for non persistent processes.

Long term plans:
* Hyarchical petri nets
* Timed petri nets
* Process migration
* Analysis (using apache Spark or apache Flink), some examples:
  * Path querying
  * Path prediction
  * Transition timing
* Visual process editor


