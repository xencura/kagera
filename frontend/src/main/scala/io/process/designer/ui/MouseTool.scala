package io.process.designer.ui

trait MouseTool[S] {
  type Transform = PartialFunction[MouseEvent, S]

  def onMouseEvent: S => Transform

  def or(other: MouseTool[S]) = {
    val self = this
    new MouseTool[S] {
      override def onMouseEvent: (S) => PartialFunction[MouseEvent, S] = { state =>
        val fn1 = self.onMouseEvent(state)
        val fn2 = other.onMouseEvent(state)
        fn1.orElse(fn2)
      }
    }
  }

  //  def and[B](other:MTool[B]): MTool[(S,B)] = {
  //    val self = this
  //    new MTool[(S,B)] {
  //      override def onMouseEvent: (S,B) => PartialFunction[MouseEvent, (S,B)] = { (a, b) =>
  //        val fn1 = self.onMouseEvent(a)
  //        val fn2 = other.onMouseEvent(b)
  //        fn1.orElse(fn2)
  //      }
  //    }
  //  }
}
