package io.process.statebox.process

import io.process.statebox.PetriNet

trait ProcessModel extends PetriNet {

  /**
   * The model's symbolic name.
   *
   * @return
   */
  def symbolicName: String

  /**
   * The model's version.
   *
   * @return
   */
  def version: String
}
