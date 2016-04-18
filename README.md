# Kagera Petri net process DSL & executor

Petri net specification example:

```
val sum = {

    // places
    val a = Place[Int](id = 1, label = "a")
    val b = Place[Int](id = 2, label = "b")
    val c = Place[Int](id = 3, label = "result")

    // transitions
    val init = TFn(id = 1, label = "init") {
      () => (5, 5)
    }

    val sum = TFn(id = 2, label = "sum") {
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
* Persistence (with persistent actors)
* Visual state using GraphViz
* Clustering & Sharding
* Other execution models, splitting processes over multiple actors

Long term plans:
* Process migration
* Analysis, path querying 
* Path prediction
* Visual process editor


