package io.kagera.api.colored

import scalax.collection.GraphEdge.{ DiEdgeLike, EdgeCopy }
import scalax.collection.GraphPredef.OuterEdge
import scalax.collection.edge.WBase.WEdgeCompanion
import scalax.collection.edge.WUnDiEdge

class Arc[N](nodes: Product, weight: Long)
    extends WUnDiEdge[N](nodes, weight)
    with DiEdgeLike[N]
    with EdgeCopy[Arc]
    with OuterEdge[N, Arc] {
  override def copy[NN](newNodes: Product) = new Arc[NN](newNodes, weight)
}

object Arc extends WEdgeCompanion[Arc] {
  override protected def newEdge[N](nodes: Product, weight: Long) = new Arc[N](nodes, weight)
}
