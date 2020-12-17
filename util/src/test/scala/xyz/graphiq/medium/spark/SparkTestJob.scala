package xyz.graphiq.medium.spark

import org.apache.spark.sql.SparkSession

trait SparkTestJob extends SparkJob {

  // Limit the number of cores and disable the UI
  implicit override lazy val spark: SparkSession = SparkSession
    .builder()
    .config(sparkConf)
    .config("spark.ui.enabled", false)
    .master("local[*]")
    .appName(appName)
    .getOrCreate()

  lazy val testDataPath: String = getClass.getResource(s"/$appName").getPath
}
