package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show

object DefaultCypherShows {
  final val defaultMaxLineWidth: Int = 120
}

trait DefaultCypherShows {

  protected val maxLineWidth: Int = DefaultCypherShows.defaultMaxLineWidth

  private def separateWithNewLines(strs: Iterable[String]): Boolean = {
    // Sum the lengths until we know whether this is too big to fit on one line
    var totalLength = 0
    val strIter = strs.iterator
    while (strIter.hasNext && totalLength < maxLineWidth) {
      totalLength += strIter.next().length + 2
    }
    // If there are still more values and not enough space, separate with newlines
    strIter.hasNext
  }

  implicit lazy val showCypherString: Show[CypherString] = Show.show(str => "\"" + str.value + "\"")
  implicit lazy val showCypherInt: Show[CypherInt] = Show.fromToString
  implicit lazy val showCypherBoolean: Show[CypherBoolean] = Show.fromToString
  implicit lazy val showCypherDouble: Show[CypherDouble] = Show.show(d => s"${d}D")
  implicit lazy val showCypherLong: Show[CypherLong] = Show.show(l => s"${l}L")
  implicit lazy val showCypherFloat: Show[CypherFloat] = Show.show(f => s"${f}F")
  implicit lazy val showCypherShort: Show[CypherShort] = Show.show(s => s"(short)$s")
  implicit lazy val showCypherByte: Show[CypherByte] = Show.show(b => s"(byte)$b")
  implicit lazy val showCypherChar: Show[CypherChar] = Show.show(c => s"'$c'")

  implicit lazy val showCypherPrimitive: Show[CypherPrimitive] = Show.show {
    case v: CypherString => Show[CypherString].show(v)
    case v: CypherInt => Show[CypherInt].show(v)
    case v: CypherBoolean => Show[CypherBoolean].show(v)
    case v: CypherDouble => Show[CypherDouble].show(v)
    case v: CypherLong => Show[CypherLong].show(v)
    case v: CypherFloat => Show[CypherFloat].show(v)
    case v: CypherShort => Show[CypherShort].show(v)
    case v: CypherChar => Show[CypherChar].show(v)
    case v: CypherByte => Show[CypherByte].show(v)
  }

  implicit def showCypherSeq[T <: CypherValue](implicit showCypherValue: Show[CypherValue]): Show[Seq[T]] = {
    Show.show { values =>
      val asStrings = values.map(showCypherValue.show)
      if (separateWithNewLines(asStrings)) {
        "[\n" + asStrings.map("  " + _).mkString(",\n") + "\n]"
      }
      else {
        s"[ ${asStrings.mkString(", ")} ]"
      }
    }
  }

  implicit def showCypherArray[T <: CypherPrimitive](implicit showCypherPrimitive: Show[CypherPrimitive]): Show[CypherArray[T]] = {
    val showSeq = showCypherSeq[T]
    Show.show { case CypherArray(values) =>
      showSeq.show(values)
    }
  }

  implicit def showCypherValue(implicit showCypherPrimitive: Show[CypherPrimitive]): Show[CypherValue] = {
    Show.show {
      case prim: CypherPrimitive =>
        showCypherPrimitive.show(prim)
      case array: CypherArray[_] =>
        showCypherArray(showCypherPrimitive).show(array.asInstanceOf[CypherArray[CypherPrimitive]])
    }
  }

  implicit def showCypherProps(implicit showValue: Show[CypherValue]): Show[CypherProps] = {
    Show.show { props =>
      val asStrings = props.map { case (k, v) => "\"" + k + "\": " + showValue.show(v) }
      if (separateWithNewLines(asStrings)) {
        s"CypherProps {\n${asStrings.map("  " + _).mkString(",\n")}\n}"
      }
      else {
        s"CypherProps { ${asStrings.mkString(", ")} }"
      }
    }
  }

  implicit def showCypherParams(implicit showProps: Show[CypherProps]): Show[CypherParams] = {
    Show.show { params =>
      val asStrings = params.map { case (k, props) => "\"" + k + "\": " + showProps.show(props) }
      if (separateWithNewLines(asStrings)) {
        s"CypherParams {\n${asStrings.map("  " + _).mkString(",\n")}\n}"
      }
      else {
        s"CypherParams { ${asStrings.mkString(", ")} }"
      }
    }
  }
}
