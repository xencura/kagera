package io.kagera.persistence

/**
 * Trait responsible for (de)serializing token values and transition output objects.
 */
trait ObjectSerializer {

  def serializeObject(obj: AnyRef): Option[SerializedData]

  def deserializeObject(data: Option[SerializedData]): AnyRef
}
