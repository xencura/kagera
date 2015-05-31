package io.process.designer.model

import io.process.designer.ui._
import io.process.draw._
import io.process.geometry.{ AffineTransform, Dimensions }

object Node {

  def noui[T](e: T)(implicit dfn: BoundedDrawable[T]): Node[T, Null] = Node[T, Null]((e, null), d => s => dfn(d)(s._1))
}

case class Node[M, S](
  state: (M, S),
  drawFn: BoundedDrawable[(M, S)],
  tool: UITool[M, S] = Tools.nothing[M, S](),
  child: Option[Node[_, _]] = None
) {

  val (m, t) = state

  def onEvent(e: UIEvent): Node[M, S] = {
    tool(t).lift(e) match {
      case None => child.map(c => Node[M, S](state, drawFn, tool, Some(c.onEvent(e)))).getOrElse(this)
      case Some(action) =>
        val updated = action.run(m)
        if (updated != state) Node[M, S](updated, drawFn, tool) else this
    }
  }

  def ~>(child: Node[_, _]) = Node(state, drawFn, tool, Some(child))

  def seq: Seq[Node[_, _]] = this +: child.map(_.seq).getOrElse(Seq.empty)

  def draw(dimensions: Dimensions): Drawing = drawFn(dimensions)(state)
}
