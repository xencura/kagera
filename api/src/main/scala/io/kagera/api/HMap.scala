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

package io.kagera.api

/**
 * Simple Heterogeneous map implementation.
 *
 * @param data
 *   The map backing this heterogeneous map
 *
 * @tparam K
 * @tparam V
 */
case class HMap[K[_], V[_]](data: Map[K[_], V[_]]) extends Iterable[(K[_], V[_])] {

  /**
   * Returns the optional value related to the provided key.
   *
   * @param key
   *   The key.
   * @tparam T
   *   The type of the key.
   * @return
   */
  def get[T](key: K[T]): Option[V[T]] = data.get(key).map(_.asInstanceOf[V[T]])

  /**
   * Returns the value associated with the provided key or else returns the provided value.
   *
   * @param key
   *   The key.
   * @param default
   *   The default value.
   * @tparam T
   *   The type of the key (& value)
   * @return
   */
  def getOrElse[T](key: K[T], default: V[T]): V[T] = get(key).getOrElse(default)

  /**
   * Returns the value associated with the provided key or throws a no such element exception.
   *
   * @param key
   * @tparam T
   *   The type of the key
   * @return
   *   The value associated with the key.
   */
  def apply[T](key: K[T]): V[T] = get(key).get

  /**
   * Checks if a key exists in the map.
   *
   * @param key
   *   The key.
   * @return
   *   Whether or not it exists in this map.
   */
  def contains(key: K[_]) = data.contains(key)

  /**
   * The key set of this map.
   *
   * @return
   *   The key set.
   */
  def keySet: Set[K[_]] = data.keySet

  /**
   * Adds a key value pair related in type T
   *
   * @param tuple
   *   The key / value pair.
   * @tparam T
   *   The type of the pair.
   * @return
   *   The updated map.
   */
  def +[T](tuple: (K[T], V[T])): HMap[K, V] = HMap[K, V](data + (tuple._1 -> tuple._2))

  /**
   * Removes a key and associated value from this map.
   *
   * @param key
   *   They key.
   * @tparam T
   *   The type of the key.
   * @return
   *   The updated map.
   */
  def -[T](key: K[T]): HMap[K, V] = HMap[K, V](data - key)

  def updatedWith[T](key: K[T])(remappingFunction: (Option[V[T]]) => Option[V[T]]) = {
    val previousValue = get(key)
    val newValue = remappingFunction(previousValue)
    if (previousValue != newValue)
      newValue.map(nv => this - key + (key -> nv)).getOrElse(this - key)
    else
      this
  }
  override def iterator: Iterator[(K[_], V[_])] = data.iterator
}
