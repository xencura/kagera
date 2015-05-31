package io.process.designer.ui

object Tools {

  def nothing[M, S](): UITool[M, S] = { s => Map.empty }
}
