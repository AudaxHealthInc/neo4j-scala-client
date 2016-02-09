package me.jeffmay.util

object Classes {

  /**
    * Returns a dot separated name for the object's class.
    */
  final def nameOf(obj: AnyRef): String = {
    val className = obj.getClass.getName
    val lastDotIdx = className.lastIndexOf('.') + 1
    val firstDollarAfterLastDotIdx = className.indexOf('$', lastDotIdx)
    className.substring(lastDotIdx, firstDollarAfterLastDotIdx)
  }
}
