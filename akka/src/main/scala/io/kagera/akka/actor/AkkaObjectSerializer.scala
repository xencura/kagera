package io.kagera.akka.actor

import akka.actor.ActorSystem
import akka.serialization.{ SerializationExtension, SerializerWithStringManifest }
import com.google.protobuf.ByteString
import io.kagera.api._
import io.kagera.persistence.SerializedData
import io.kagera.persistence.ObjectSerializer

import scala.runtime.BoxedUnit

trait AkkaObjectSerializer extends ObjectSerializer {

  def system: ActorSystem

  private lazy val serialization = SerializationExtension.get(system)

  override def serializeObject(obj: AnyRef) = {
    (!obj.isInstanceOf[Unit]).option {
      // for now we re-use akka Serialization extension for pluggable serializers
      val serializer = serialization.findSerializerFor(obj)

      val bytes = serializer.toBinary(obj)

      val manifest = serializer match {
        case s: SerializerWithStringManifest => s.manifest(obj)
        case _ => obj.getClass.getName
      }

      // we should not have to copy the bytes
      SerializedData(
        serializerId = Some(serializer.identifier),
        manifest = Some(ByteString.copyFrom(manifest.getBytes)),
        data = Some(ByteString.copyFrom(bytes))
      )
    }
  }

  override def deserializeObject(data: Option[SerializedData]): AnyRef = {
    data
      .map {
        case SerializedData(None, _, Some(data)) =>
          throw new IllegalStateException(s"Missing serializer id")
        case SerializedData(Some(serializerId), _, Some(data)) =>
          val serializer = serialization.serializerByIdentity.getOrElse(
            serializerId,
            throw new IllegalStateException(s"No serializer found with id $serializerId")
          )
          serializer.fromBinary(data.toByteArray)
      }
      .getOrElse(BoxedUnit.UNIT)
  }
}
