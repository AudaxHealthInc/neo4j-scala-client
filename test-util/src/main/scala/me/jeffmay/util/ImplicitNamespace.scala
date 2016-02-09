package me.jeffmay.util

import java.util.concurrent.atomic.AtomicInteger

class Namespace(val value: String) extends AnyVal

trait ImplicitNamespace {
  implicit def namespace: Namespace
}

trait UniquePerClassNamespace {

  private[this] lazy val thisClassName = getClass.getSimpleName

  private[this] val n: AtomicInteger = new AtomicInteger(0)

  protected def standardize(unique: String): String = s"${thisClassName}_$unique"

  protected def nextNamespace(): String = standardize(n.incrementAndGet().toString)

  protected trait UniqueNamespace extends ImplicitNamespace {

    implicit val namespace: Namespace = new Namespace(nextNamespace())
  }
}
