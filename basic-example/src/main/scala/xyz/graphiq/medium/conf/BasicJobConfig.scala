package xyz.graphiq.medium.conf

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import xyz.graphiq.medium.generator.FileGenerator

case class BasicJobConfig(
    fileGenerator: FileGenerator
)

object BasicJobConfig {

  def apply(): BasicJobConfig =
    ConfigSource.default.at("basic-job").loadOrThrow[BasicJobConfig]

}
