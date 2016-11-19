package io.kagera.persistence

import io.kagera.persistence.messages.SerializedData

/**
 * Trait responsible for (de)serializing token values and transition output objects.
 */
trait ObjectSerializer {

  def serializeObject(obj: AnyRef): SerializedData

  def deserializeObject(data: SerializedData): AnyRef
}
