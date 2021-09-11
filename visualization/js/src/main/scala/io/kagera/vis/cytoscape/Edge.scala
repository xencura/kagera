package io.kagera.vis.cytoscape

case class Edge(id: String, source: String, target: String, edgeStyle: EdgeStyle = EdgeStyle.default)
