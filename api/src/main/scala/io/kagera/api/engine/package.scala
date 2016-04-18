package io.kagera.api

package object engine {

  def stepFirst[P, T, M]: Step[P, T, M] = (process, marking) => {
    process.enabledParameters(marking).headOption.map { case (t, enabledMarkings) => (t, enabledMarkings.head) }
  }

  def stepRandom[P, T, M]: Step[P, T, M] = (process, marking) => {
    import scalaz.syntax.std.boolean._
    import scala.util.Random

    val params = process.enabledParameters(marking)

    params.nonEmpty.option {
      val n = Random.nextInt(Math.min(10, params.size))
      val (t, enabledMarkings) = Stream.continually(params.toStream).flatten.apply(n)
      (t, enabledMarkings.head)
    }
  }

}
