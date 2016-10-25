package io.kagera.persistence

trait ObjectSerializer {

  def serializeObject(obj: AnyRef): Option[SerializedData]

  def deserializeObject(data: Option[SerializedData]): AnyRef
}
