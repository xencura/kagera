package io.process.common.draw.ui

object Tools {

  def nothing[M, S](): UITool[M, S] = { s => Map.empty }
}
