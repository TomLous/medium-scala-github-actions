// Format Scala code https://github.com/scalameta/scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.1")

// Automated release process https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

// sbt plugin for Scala Code Coverage: https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// sbt pliugin for linting: https://github.com/scalastyle/scalastyle-sbt-plugin
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Deploy fat JARs. Restart processes: https://github.com/sbt/sbt-assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

// sbt plugin for dockerization: https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.24")

// sbt plugin to build Docker Images: https://github.com/marcuslonnberg/sbt-docker
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

// sbt plugin to check dependency updates: https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
