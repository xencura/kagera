package io.process.designer.model

import io.process.common.draw._
import io.process.common.draw.ui._
import io.process.common.geometry._

import scalaz.State

object Node {

  case class NodeState[S](affineTransform: AffineTransform, state: S, drawFn: BoundedDrawable[S])

  type NodeT[M, S, A] = State[Node[M, S], A]

  def set[T](set: Set[T])(implicit draw: Drawable[T]) =
    Node[Set[T], Null]((set, null), d => set => drawIterable(draw)(set))

  def noui[T](e: T)(implicit dfn: BoundedDrawable[T]): Node[T, Null] = Node[T, Null]((e, null), d => s => dfn(d)(s))
}

case class Node[M, S](
  state: (M, S),
  drawState: BoundedDrawable[M],
  tool: UITool[M, S] = Tools.nothing[M, S](),
  child: Option[Node[_, _]] = None
) {

  val (m, t) = state

  def onEvent(e: UIEvent): Node[M, S] = {

    tool(t).lift(e) match {
      case None => child.map(c => Node[M, S](state, drawState, tool, Some(c.onEvent(e)))).getOrElse(this)
      case Some(action) =>
        val updated = action.run(m)
        if (updated != state) Node[M, S](updated, drawState, tool, child) else this
    }
  }

  def withTool[T](toolState: T, tool: UITool[M, T]): Node[M, T] = Node((state._1, toolState), drawState, tool, child)

  def ~>(child: Node[_, _]) = Node(state, drawState, tool, Some(child))

  def seq: Seq[Node[_, _]] = this +: child.map(_.seq).getOrElse(Seq.empty)

  def draw(dimensions: Dimensions): Drawing = drawState(dimensions)(state._1)
}
