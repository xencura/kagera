package io.kagera.api.colored

import scala.concurrent.Future
import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.universe.TypeTag
import scalax.collection.edge.WLDiEdge

case class ReflectedTransition[I : TypeTag, O : TypeTag](
  override val id: Long,
  override val label: String,
  override val isManaged: Boolean,
  fn: I => O
) extends Transition {

  private lazy val universeMirror = ru.runtimeMirror(getClass.getClassLoader)

  type Input = I
  type Output = O

  // get first constructor
  val runtimeClass = universeMirror.runtimeClass(ru.typeOf[Input]).asInstanceOf[Class[Input]]
  val constructor = runtimeClass.getConstructors.apply(0)

  val names = implicitly[TypeTag[Input]].tpe
    .member(ru.termNames.CONSTRUCTOR)
    .asMethod
    .paramLists(0)
    .map(_.name.decodedName.toString)

  def createInput(input: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input = {

    val constructorInput = input
      .map { case (place, arc, data) => (names.indexWhere(_ == place.label), data.head) }
      .sortBy { case (index, data) => index }
      .map { case (index, data) => data.asInstanceOf[AnyRef] }

    constructor.newInstance(constructorInput: _*).asInstanceOf[Input]
  }

  def createOutput(output: Output, outMarking: Seq[(WLDiEdge[Node], Place)]): ColoredMarking = {

    outMarking.map { case (arc, place) =>
      val value = runtimeClass.getDeclaredField(place.label).get(output)
      place -> Seq(value)
    }.toMap
  }

  override def apply(input: Input): Future[Output] = Future.successful(fn(input))
}
