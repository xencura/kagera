/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
