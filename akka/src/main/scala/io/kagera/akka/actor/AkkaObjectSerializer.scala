package io.kagera.akka.actor

import akka.actor.ActorSystem
import akka.serialization.{ SerializationExtension, SerializerWithStringManifest }
import com.google.protobuf.ByteString
import io.kagera.persistence.ObjectSerializer
import io.kagera.persistence.messages._

class AkkaObjectSerializer(system: ActorSystem) extends ObjectSerializer {

  private val serialization = SerializationExtension.get(system)

  override def serializeObject(obj: AnyRef) = {
    // for now we re-use akka Serialization extension for pluggable serializers
    val serializer = serialization.findSerializerFor(obj)

    val bytes = serializer.toBinary(obj)

    val manifest = serializer match {
      case s: SerializerWithStringManifest => s.manifest(obj)
      case _ => if (obj != null) obj.getClass.getName else ""
    }

    // we should not have to copy the bytes
    SerializedData(
      serializerId = Some(serializer.identifier),
      manifest = Some(ByteString.copyFrom(manifest.getBytes)),
      data = Some(ByteString.copyFrom(bytes)),
      unknownFields = scalapb.UnknownFieldSet()
    )
  }

  override def deserializeObject(data: SerializedData): AnyRef = {
    data match {
      case SerializedData(None, _, Some(data), _) =>
        throw new IllegalStateException(s"Missing serializer id")
      case SerializedData(Some(serializerId), _, Some(data), _) =>
        val serializer = serialization.serializerByIdentity.getOrElse(
          serializerId,
          throw new IllegalStateException(s"No serializer found with id $serializerId")
        )
        serializer.fromBinary(data.toByteArray)
    }
  }
}
