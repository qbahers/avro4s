package com.sksamuel.avro4s

import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.{Instant, LocalDate, LocalDateTime, OffsetDateTime}
import java.util
import java.util.{Date, UUID}

import org.apache.avro.util.Utf8
import org.apache.avro.{LogicalType, LogicalTypes, Schema, SchemaBuilder}

import scala.deriving.Mirror

object AvroSchema {
  def apply[T](using schemaFor: SchemaFor[T]): Schema = schemaFor.schema[T]
}

/**
 * A [[SchemaFor]] generates an Avro Schema for a Scala or Java type.
 *
 * For example, a SchemaFor[String] could return a schema of type Schema.Type.STRING, and
 * a SchemaFor[Int] could return an schema of type Schema.Type.INT
 */
trait SchemaFor[T]:

  def schema[T]: Schema

  /**
   * Changes the type of this SchemaFor to the desired type `U` without any other modifications.
   *
   * @tparam U new type for SchemaFor.
   */
  def forType[U]: SchemaFor[U] = map[U](identity)

  /**
   * Creates a SchemaFor[U] by applying a function Schema => Schema
   * to the schema generated by this instance.
   */
  def map[U](fn: Schema => Schema): SchemaFor[U] = ???

object SchemaFor extends BaseSchemas {

  def apply[T](s: Schema): SchemaFor[T] = new SchemaFor[T] {
    override def schema[T]: Schema = s
  }

  inline given derived[T](using m: Mirror.Of[T]) : SchemaFor[T] = SchemaForMacros.derive[T]

  //  inline def schemaForProduct[T](p: Mirror.Product, recordName: String, elems: List[SchemaFor[_]], labels: List[String]): SchemaFor[T] = {
  //
  //    val fields = new util.ArrayList[Schema.Field]()
  //    elems.zip(labels).foreach { case (fieldSchemaFor, fieldName) =>
  //      val field = new Schema.Field(fieldName, fieldSchemaFor.schema, null)
  //      fields.add(field)
  //    }
  //    // todo use schema builder once cyclic reference bug in dotty is fixed
  //    val _schema = Schema.createRecord(recordName, null, "mynamespace", false, fields)
  //    new SchemaFor[T] :
  //      override def schema[T]: Schema = _schema
  //  }

  //  inline def labelsToList[T <: Tuple]: List[String] =
  //    inline erasedValue[T] match {
  //      case _: Unit => Nil
  //      case _: (head *: tail) => (inline constValue[head] match {
  //        case str: String => str
  //        case other => other.toString
  //      }) :: labelsToList[tail]
  //      // todo why is this Any required, why doesn't Unit grab the empty type?
  //      case _: Any => Nil
  //    }
  //
  //  inline def summonAll[T]: List[SchemaFor[_]] = inline erasedValue[T] match {
  //    case _: EmptyTuple => Nil
  //    case _: (t *: ts) => summonInline[SchemaFor[t]] :: summonAll[ts]
  //  }
  //
  //  inline given derived[T](using m: Mirror.Of[T]) as SchemaFor[T] = {
  //    val elemInstances = summonAll[m.MirroredElemTypes]
  //    val labels = labelsToList[m.MirroredElemLabels]
  //    val name = inline constValue[m.MirroredLabel] match {
  //      case str: String => str
  //    }
  //    inline m match {
  //      case s: Mirror.SumOf[T] => ???
  //      case p: Mirror.ProductOf[T] => schemaForProduct(p, name, elemInstances, labels)
  //    }
  //  }
}


trait BaseSchemas {

  given intSchemaFor: SchemaFor[Int] = SchemaFor[Int](SchemaBuilder.builder.intType)
  given SchemaFor[Byte] = intSchemaFor.forType
  given SchemaFor[Short] = intSchemaFor.forType
  given SchemaFor[Long] = SchemaFor[Long](SchemaBuilder.builder.longType)
  given SchemaFor[Float] = SchemaFor[Float](SchemaBuilder.builder.floatType)
  given SchemaFor[Double] = SchemaFor[Double](SchemaBuilder.builder.doubleType)
  given SchemaFor[scala.Boolean] = SchemaFor[Boolean](SchemaBuilder.builder.booleanType)
  given SchemaFor[ByteBuffer] = SchemaFor[ByteBuffer](SchemaBuilder.builder.bytesType)
  given stringSchemaFor: SchemaFor[String] = SchemaFor[String](SchemaBuilder.builder.stringType)
  given SchemaFor[Utf8] = stringSchemaFor.forType
  given SchemaFor[CharSequence] = stringSchemaFor.forType
  given SchemaFor[UUID] = SchemaFor[UUID](LogicalTypes.uuid().addToSchema(SchemaBuilder.builder.stringType))
}

object TimestampNanosLogicalType extends LogicalType("timestamp-nanos") {
  override def validate(schema: Schema): Unit = {
    super.validate(schema)
    if (schema.getType != Schema.Type.LONG) {
      throw new IllegalArgumentException("Logical type timestamp-nanos must be backed by long")
    }
  }
}

object OffsetDateTimeLogicalType extends LogicalType("datetime-with-offset") {
  override def validate(schema: Schema): Unit = {
    super.validate(schema)
    if (schema.getType != Schema.Type.STRING) {
      throw new IllegalArgumentException("Logical type iso-datetime with offset must be backed by String")
    }
  }
}

trait DateSchemas {
  given InstantSchemaFor : SchemaFor[Instant] = SchemaFor[Instant](LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder.longType))
  given DateSchemaFor : SchemaFor[Date] = SchemaFor(LogicalTypes.date().addToSchema(SchemaBuilder.builder.intType))
  given LocalDateSchemaFor : SchemaFor[LocalDate] = DateSchemaFor.forType
  given LocalDateTimeSchemaFor : SchemaFor[LocalDateTime] = SchemaFor(TimestampNanosLogicalType.addToSchema(SchemaBuilder.builder.longType))
  given OffsetDateTimeSchemaFor : SchemaFor[OffsetDateTime] = SchemaFor(OffsetDateTimeLogicalType.addToSchema(SchemaBuilder.builder.stringType))
  given LocalTimeSchemaFor : SchemaFor[Nothing] = SchemaFor(LogicalTypes.timeMicros().addToSchema(SchemaBuilder.builder.longType))
  given TimestampSchemaFor : SchemaFor[Timestamp] = SchemaFor[Timestamp](LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder.longType))
}

trait ByteIterableSchemas {
  given ByteArraySchemaFor: SchemaFor[Array[Byte]] = SchemaFor[Array[Byte]](SchemaBuilder.builder.bytesType)
  given ByteListSchemaFor: SchemaFor[List[Byte]] = ByteArraySchemaFor.forType
  given ByteSeqSchemaFor: SchemaFor[Seq[Byte]] = ByteArraySchemaFor.forType
  given ByteVectorSchemaFor: SchemaFor[Vector[Byte]] = ByteArraySchemaFor.forType
}
