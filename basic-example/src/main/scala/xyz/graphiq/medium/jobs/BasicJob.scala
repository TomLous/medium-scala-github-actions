package xyz.graphiq.medium.jobs

import xyz.graphiq.medium.conf.BasicJobConfig
import xyz.graphiq.medium.spark.SparkJob

object BasicJob extends App with SparkJob {

  logger.debug("Reading config")

  implicit val config: BasicJobConfig = BasicJobConfig()

  logger.info(
    s"Creating a DataFrame with ${config.fileGenerator.rows} rows, ${config.fileGenerator.columns} columns and ${config.fileGenerator.partitions} partitions"
  )

  val randomDataFrame = config.fileGenerator.toDF
  randomDataFrame.show()

  logger.info("Summarizing")

  randomDataFrame.summary("count", "min", "25%", "75%", "max").show()

  logger.info("Done")

}
