package com.wenjunhuang.codeepiphany.utils

import scala.collection.mutable

class LRUCache[K, V](val capacity: Int) extends Iterable[(K, V)] {
  private val cache = mutable.LinkedHashMap[K, V]()

  def get(key: K): Option[V] =
    cache.get(key).map { value =>
      cache.remove(key)
      cache.put(key, value)
      value
    }

  def put(key: K, value: V): Option[(K, V)] = {
    cache.put(key, value)

    if cache.size > capacity then
      val rk = cache.head._1
      cache.remove(rk).map((rk, _))
    else None
  }

  def clear(): Unit = cache.clear()

  override def iterator: Iterator[(K, V)] = cache.iterator
}
