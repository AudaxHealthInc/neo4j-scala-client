package me.jeffmay.neo4j.client.cypher

import org.scalatest.WordSpec

class CypherWritesSpec extends WordSpec {

  "CypherWrites" should {

    "allow writing a simple String wrapper class the same as a String" in {
      val value = "value"
      assertResult(Cypher.props("id" -> value)) {
        Cypher.props("id" -> SimpleString(value))
      }
    }

    "allow writing a simple Boolean wrapper class same as a Boolean" in {
      val value = true
      assertResult(Cypher.props("id" -> value)) {
        Cypher.props("id" -> SimpleBoolean(value))
      }
    }

    "allow writing a simple Int wrapper class same as an Int" in {
      val value = 1
      assertResult(Cypher.props("id" -> value)) {
        Cypher.props("id" -> SimpleInt(value))
      }
    }

    "allow writing a simple Long wrapper class same as an Long" in {
      val value = 1L
      assertResult(Cypher.props("id" -> value)) {
        Cypher.props("id" -> SimpleLong(value))
      }
    }

    "allow writing a simple Double wrapper class same as an Double" in {
      val value = 2.0D
      assertResult(Cypher.props("id" -> value)) {
        Cypher.props("id" -> SimpleDouble(value))
      }
    }
  }

  "CypherWritesProps" should {

    "allow writing a simple case class the same as a map with the same fields" in {
      val id = 1
      val value = "value"
      assertResult(Cypher.props("id" -> id, "value" -> value)) {
        Cypher.toProps(SimpleObject(id, value))
      }
    }
  }
}

case class SimpleString(value: String)
object SimpleString {
  implicit val simpleWrites: CypherWrites.AsString[SimpleString] = CypherWrites.a[SimpleString].asString(_.value)
}

case class SimpleBoolean(value: Boolean)
object SimpleBoolean {
  implicit val simpleWrites: CypherWrites.AsBoolean[SimpleBoolean] = CypherWrites.a[SimpleBoolean].using(_.value)
}

case class SimpleInt(value: Int)
object SimpleInt {
  implicit val simpleWrites: CypherWrites.AsInt[SimpleInt] = CypherWrites.a[SimpleInt].using(_.value)
}

case class SimpleLong(value: Long)
object SimpleLong {
  implicit val simpleWrites: CypherWrites.AsLong[SimpleLong] = CypherWrites.a[SimpleLong].using(_.value)
}

case class SimpleDouble(value: Double)
object SimpleDouble {
  implicit val simpleWrites: CypherWrites.AsDouble[SimpleDouble] = CypherWrites.a[SimpleDouble].using(_.value)
}

case class SimpleObject(id: Int, value: String)
object SimpleObject {
  implicit val simpleWritesProps: CypherWrites.AsProps[SimpleObject] = {
    CypherWrites.a[SimpleObject].asProps(o => Cypher.props("id" -> o.id, "value" -> o.value))
  }
}
