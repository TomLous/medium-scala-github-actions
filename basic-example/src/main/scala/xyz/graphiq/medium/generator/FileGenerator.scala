package xyz.graphiq.medium.generator

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._
import scala.collection.JavaConverters._

import scala.util.Random

case class FileGenerator(
    rows: Int,
    columns: Int,
    partitions: Int = FileGenerator.DEFAULT_PARTITIONS
) {

  lazy val schema: StructType = generateSchema
  private val maxFieldNameLength: Int = 15
  private val maxNum: Int = 20

  private val typeGenF: Map[DataType, Int => Any] = Map(
    (StringType, (i: Int) => Random.nextString(i.abs + 1)),
    (DoubleType, (_: Int) => Random.nextDouble()),
    (IntegerType, (i: Int) => Random.nextInt(i.abs + 1)),
    (LongType, (_: Int) => Random.nextLong()),
    (BooleanType, (_: Int) => Random.nextBoolean())
  )

  private def generateSchema: StructType = {
    StructType((0 until columns).map { _ =>
      val fieldNamelength = Random.nextInt(maxFieldNameLength).abs + 1
      val fieldName = Random.alphanumeric.take(fieldNamelength).mkString
      val fieldType = typeGenF.toList(Random.nextInt(typeGenF.size))._1
      StructField(fieldName, fieldType)
    })
  }

  def toDF(implicit spark: SparkSession): DataFrame = {
    val records = (0 until rows).map(_ =>
      Row.fromSeq(
        (0 until columns).map(c => {
          val a = Random.nextInt(maxNum)
          val b = schema.fields(c).dataType
          val f = typeGenF(b)
          f(a)
        })
      )
    )

    spark.createDataFrame(records.asJava, schema).repartition(partitions)

  }
}

object FileGenerator {
  val DEFAULT_PARTITIONS: Int = 200
}
