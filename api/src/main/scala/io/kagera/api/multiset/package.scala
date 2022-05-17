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

package object multiset {

  type MultiSet[T] = Map[T, Int]

  object MultiSet {

    def empty[T]: MultiSet[T] = Map.empty[T, Int]

    def from[T](elements: Iterable[T]): MultiSet[T] = elements.foldLeft(empty[T]) { case (mset, e) =>
      mset.multisetIncrement(e, 1)
    }

    def apply[T](elements: T*): MultiSet[T] = from(elements.toSeq)
  }

  implicit class MultiSetOps[T](val mset: MultiSet[T]) extends AnyVal {
    def multisetDifference(other: MultiSet[T]): MultiSet[T] =
      other.foldLeft(mset) { case (result, (element, count)) =>
        result.multisetDecrement(element, count)
      }

    def multisetSum(other: MultiSet[T]): MultiSet[T] =
      other.foldLeft(mset) { case (m, (p, count)) =>
        m.get(p) match {
          case None => m + (p -> count)
          case Some(n) => m + (p -> (n + count))
        }
      }

    def isSubSet(other: MultiSet[T]): Boolean =
      !other.exists { case (element, count) =>
        mset.get(element) match {
          case None => true
          case Some(n) if n < count => true
          case _ => false
        }
      }

    def multisetSize: Int = mset.values.sum

    def multiplicities(map: MultiSet[T]): Map[T, Int] = map

    def setMultiplicity(map: Map[T, Int])(element: T, m: Int): Map[T, Int] = map + (element -> m)

    def allElements: Iterable[T] = mset.foldLeft(List.empty[T]) { case (list, (e, count)) =>
      List.fill[T](count)(e) ::: list
    }

    def multisetDecrement(element: T, count: Int): MultiSet[T] =
      mset.get(element) match {
        case None => mset
        case Some(n) if n <= count => mset - element
        case Some(n) => mset + (element -> (n - count))
      }

    def multisetIncrement(element: T, count: Int): MultiSet[T] = mset + (element -> (1 + mset.getOrElse(element, 0)))
  }
}
