package io.process.designer.ui

object MouseTool {

  def nothing[T, S]() = new MouseTool[T, S] {
    override def onMouseEvent: (T, S) => Transform = (m, s) => Map.empty
  }
}

trait MouseTool[M, S] {

  type Transform = PartialFunction[MouseEvent, (M, S)]

  def onMouseEvent: (M, S) => Transform

  def join(other: MouseTool[M, S]) = {
    val self = this
    new MouseTool[M, S] {
      override def onMouseEvent = { (model, state) =>
        val fn1 = self.onMouseEvent(model, state)
        val fn2 = other.onMouseEvent(model, state)
        fn1.orElse(fn2)
      }
    }
  }

  def or[B](other: MouseTool[M, B]) = {
    val self = this
    new MouseTool[M, (S, B)] {
      override def onMouseEvent = { (m, state) =>
        val (a, b) = state
        val fn1 = self.onMouseEvent(m, a).andThen(out => (out._1, (out._2, b)))
        val fn2 = other.onMouseEvent(m, b).andThen(out => (out._1, (a, out._2)))
        fn1.orElse(fn2)
      }
    }
  }
}
