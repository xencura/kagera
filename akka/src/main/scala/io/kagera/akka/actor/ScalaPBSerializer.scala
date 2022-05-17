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

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import scalapb.{ GeneratedMessage, GeneratedMessageCompanion }
import io.kagera.akka.actor.ScalaPBSerializer._
import io.kagera.persistence.messages._

object ScalaPBSerializer {
  import scala.reflect.runtime.{ universe => ru }

  private lazy val universeMirror = ru.runtimeMirror(getClass.getClassLoader)

  def scalaPBType[T <: GeneratedMessage](implicit tt: ru.TypeTag[T]): (Class[T], GeneratedMessageCompanion[T]) = {
    val messageType = universeMirror.runtimeClass(ru.typeOf[T].typeSymbol.asClass).asInstanceOf[Class[T]]
    val companionType = universeMirror
      .reflectModule(ru.typeOf[T].typeSymbol.companion.asModule)
      .instance
      .asInstanceOf[GeneratedMessageCompanion[T]]
    messageType -> companionType
  }

  val UTF8: Charset = Charset.forName("UTF-8")
  val Identifier: Int = ByteBuffer.wrap("akka-scalapb-serializer".getBytes(UTF8)).getInt
}

class ScalaPBSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {

  def manifests: Map[String, (Class[_ <: AnyRef], GeneratedMessageCompanion[_ <: GeneratedMessage])] = Map(
    "TransitionFired" -> scalaPBType[TransitionFired],
    "TransitionFailed" -> scalaPBType[TransitionFailed],
    "Initialized" -> scalaPBType[Initialized]
  )

  private val class2ManifestMap: Map[Class[_ <: AnyRef], String] = manifests.map { case (key, value) =>
    value._1 -> key
  }

  override def identifier: Int = ScalaPBSerializer.Identifier

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifests
      .get(manifest)
      .map { case (_, companion) => companion.parseFrom(bytes).asInstanceOf[AnyRef] }
      .getOrElse(throw new IllegalArgumentException(s"Cannot deserialize byte array with manifest $manifest"))
  }

  override def manifest(o: AnyRef): String = {
    class2ManifestMap(o.getClass)
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case msg: GeneratedMessage =>
        val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
        msg.writeTo(stream)
        stream.toByteArray
    }
  }
}
