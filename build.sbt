import sbt.Def
import sbtdocker.Instructions

/**
  * Dependency versions.
  * When able to commit to a Spark major version, remove the switch logic below
  * TODO: change config to fixed version
  */
val sparkMajorVersion = sys.props.getOrElse("sparkMajorVersion", default = "3").toInt

val versions =
  Map(
    "scala" -> "2.12.10",
    // These have to be in sync with the base image versions
    "spark" -> "3.0.0",
    "hadoop" -> "3.2.0",
    // dependencies
    "pureconfig" -> "0.14.0",
    "scalaTest" -> "3.2.3",
    "mockito" -> "1.16.3",
    "jackson" -> "2.10.0",
    "slf4j" -> "1.7.30"
  )

val scalaBaseVersion = versions("scala").reverse.dropWhile(_ != '.').reverse.init

/**
  * Library definitions
  */
val sparkLibs = Seq(
  "org.apache.spark" %% "spark-core" % versions("spark") % "provided",
  "org.apache.spark" %% "spark-sql" % versions("spark") % "provided"
).map(_.exclude("org.slf4j", "*"))

val loggingLibs = Seq(
  "org.slf4j" % "slf4j-api" % versions("slf4j") % "provided",
  "org.slf4j" % "jul-to-slf4j" % versions("slf4j") % "provided",
  "org.slf4j" % "slf4j-log4j12" % versions("slf4j") % "provided"
)

val configLibs = Seq(
  "com.github.pureconfig" %% "pureconfig" % versions("pureconfig"),
  "com.github.pureconfig" %% "pureconfig-joda" % versions("pureconfig")
)

val testingLibs = Seq(
  "org.scalatest" %% "scalatest" % versions("scalaTest") % Test,
  "org.mockito" %% "mockito-scala" % versions("mockito") % Test,
  "org.mockito" %% "mockito-scala-scalatest" % versions("mockito") % Test
)

val jacksonLibsOverride = Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % versions("jackson"),
  "com.fasterxml.jackson.core" % "jackson-annotations" % versions("jackson"),
  "com.fasterxml.jackson.core" % "jackson-databind" % versions("jackson"),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % s"${versions("jackson")}"
)

val otherOverrides = Seq(
  "com.google.guava" % "guava" % "23.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "io.netty" % "netty" % "3.9.9.Final",
  "io.netty" % "netty-all" % "4.1.17.Final",
  "com.nimbusds" % "nimbus-jose-jwt" % "4.41.1",
  "net.minidev" % "json-smart" % "2.3"
)

/**
  * Optional list of dependencies to be packed with Docker
  * For example: Postgress libaries should be bundled with the docker to be loaded before Spark
  */
val externalDependencies = Seq.empty[ModuleID]

/**
  * Project settings
  */
val team = "ateam"
val baseImage = s"gcr.io/spark-operator/spark:v3.0.0-hadoop3"
val targetDockerJarPath = "/opt/spark/jars"
val targetDockerWorkDirPath = "/opt/spark/work-dir"
val externalPaths = dependenciesToPaths(externalDependencies)

/**
  * These settings are used in each submodule
  */
lazy val commonSettings = Seq(
  organization := "xyz.graphiq",
  scalaVersion := versions("scala"),
  libraryDependencies ++= sparkLibs ++ loggingLibs ++ configLibs ++ testingLibs,
  dependencyOverrides ++= jacksonLibsOverride ++ otherOverrides,
  dependencyUpdatesFilter -= moduleFilter(organization = "org.scala-lang"),
  dependencyUpdatesFilter -= moduleFilter(organization = "org.apache.spark"),
  dependencyUpdatesFilter -= moduleFilter(organization = "org.apache.hadoop"),
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "UTF-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
  )
)

lazy val testSettings = Seq(
  Test / testOptions += Tests.Argument("-oDT"),
  Test / parallelExecution := false
)

lazy val assemblySettings = Seq(
  // Assembly options
  assembly / assemblyOption := (assemblyOption in assembly).value.copy(includeScala = false),
  assembly / assemblyOutputPath := baseDirectory.value / "../output" / getArtifactName.value,
  assembly / assemblyMergeStrategy := {
    case PathList(ps @ _*) if ps.last.startsWith("application.") && ps.last.endsWith(".conf") =>
      MergeStrategy.concat
    case PathList(ps @ _*) if ps.last.endsWith(".yaml") || ps.last.endsWith(".properties") =>
      MergeStrategy.first
    case PathList("META-INF", _ @_*) => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last.startsWith("LICENSE") || ps.last.startsWith("NOTICE") =>
      MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  assembly / logLevel := sbt.util.Level.Error,
  assembly / test := {},
  pomIncludeRepository := { _ =>
    false
  }
)

lazy val runLocalSettings = Seq(
  // Include "provided" dependencies back to default run task
  // https://stackoverflow.com/questions/18838944/how-to-add-provided-dependencies-back-to-run-test-tasks-classpath/21803413#21803413
  Compile / run := Defaults
    .runTask(
      fullClasspath in Compile,
      mainClass in (Compile, run),
      runner in (Compile, run)
    )
    .evaluated
)

lazy val scalafmtSettings = Seq(
  scalafmtOnCompile in ThisBuild := true
)

lazy val dockerSettings = Seq(
  imageNames in docker := Seq(
    ImageName(s"$team/${name.value}:${version.value}"),
    ImageName(s"$team/${name.value}:latest")
  ),
  buildOptions in docker := BuildOptions(
    cache = false,
    removeIntermediateContainers = BuildOptions.Remove.Always,
    pullBaseImage = BuildOptions.Pull.Always
  ),
  dockerfile in docker := {
    val artifact: File = assembly.value
    val artifactTargetPath = s"$targetDockerJarPath/${getArtifactName.value}"
    externalPaths
      .map {
        case (extOrgDir, extModuleName, extVersion, jarFile) =>
          val url =
            List("https://repo1.maven.org/maven2", extOrgDir, extModuleName, extVersion, jarFile)
              .mkString("/")
          val target = s"$targetDockerJarPath/$jarFile"
          Instructions.Run.exec(List("curl", url, "--output", target, "--silent"))
      }
      .foldLeft(new Dockerfile {
        from(baseImage)
      }) {
        case (df, run) => df.addInstruction(run)
      }
      .add(artifact, artifactTargetPath)
      .add(file("README.md"), s"$targetDockerWorkDirPath/")
  }
)

/**
  * Project definition
  * Here you can define all your modules. Best practice is still to create 1 even if you don't plan on more then one
  * [module-name]\
  *         helm-vars\
  *              values-[env].yaml
  *         src\
  *           main\
  *               resources\
  *                  application.[env].conf
  *                  log4j.properties
  *                scala\
  *                   xyz.graphiq.... [code]
  *           test\
  *               resources\
  *                  application.[env].conf
  *                  log4j.properties
  *                scala\
  *                   xyz.graphiq.... [ test code]
  */
lazy val util = (project in file("util"))
  .settings(
    commonSettings,
    testSettings,
    scalafmtSettings,
    runLocalSettings,
    name := "util"
  )

lazy val `basic-example` = createProjectModule(
  "basic-example",
  "Example just running Spark",
  Some("xyz.graphiq.medium.jobs.BasicJob")
)

// The overarching project. Rename to fit github name, and make sure it accumulates all subprojects, starting with the shared ones
lazy val `medium-scala-github-actions` = (project in file("."))
  .aggregate(
    util,
    `basic-example`
  )

// Method to create complete runnable modules on the fly
def createProjectModule(
    moduleName: String,
    description: String,
    runClass: Option[String]
): Project =
  Project(moduleName, file(moduleName))
    .enablePlugins(sbtdocker.DockerPlugin)
    .enablePlugins(AshScriptPlugin)
    .dependsOn(util % "compile->compile;test->test")
    .settings(
      commonSettings,
      testSettings,
      assemblySettings,
      dockerSettings,
      runLocalSettings,
      scalafmtSettings,
      name := moduleName,
      Compile / mainClass := runClass,
      Compile / resourceGenerators += generateHelmChart(runClass, description).taskValue,
      listModules := {
        println(moduleName)
      },
      getImageName := {
        (docker / imageNames).value.headOption.map { imageName =>
          imageName.namespace.map(_ + "/").getOrElse("") + imageName.repository
        }
      },
      getChartName := {
        s"$team-$moduleName"
      },
      getArtifactName := {
        s"$team-$moduleName.jar"
      },
      showImageName := {
        println(getImageName.value.getOrElse(""))
      },
      showChartName := {
        println(getChartName.value)
      }
    )

// Method to generate the helm chart values for a certain project.
// Output Chart.yaml and  values.yaml to /helm dir. Used for CI/CD
def generateHelmChart(
    etlMainClass: Option[String],
    etlDescription: String
): Def.Initialize[Task[Seq[File]]] =
  Def.task {
    val chartFile = baseDirectory.value / "../helm" / "Chart.yaml"
    val valuesFile = baseDirectory.value / "../helm" / "values.yaml"
    val jarDependencies = externalPaths.map {
      case (_, extModuleName, _, jarFile) =>
        extModuleName -> s""""local://$targetDockerJarPath/$jarFile""""
    }.toMap

    val chartContents =
      s"""# Generated by build.sbt. Please don't manually update
         |apiVersion: v1
         |name: ${getChartName.value}
         |version: ${version.value}
         |appVersion: ${version.value}
         |description: $etlDescription
         |home: https://lous.info/
         |sources:
         |  - https://github.com/TomLous/medium-scala-github-actions
         |maintainers: 
         |  - name: Tom Lous
         |    email: tomlous@gmail.com
         |    url: https://github.com/TomLous       
         |""".stripMargin

    val valuesContents =
      s"""# Generated by build.sbt. Please don't manually update
         |version: ${version.value}
         |sparkVersion: ${versions("spark")}
         |image: ${getImageName.value.getOrElse("-")}:${version.value}
         |imageRegistry: localhost:5000
         |jar: local://$targetDockerJarPath/${getArtifactName.value}
         |mainClass: ${etlMainClass.getOrElse("")}
         |jarDependencies: [${jarDependencies.values.mkString(", ")}]
         |jmxExporterJar: /prometheus/jmx_prometheus_javaagent.jar
         |fileDependencies: []      
         |""".stripMargin

    IO.write(chartFile, chartContents)
    IO.write(valuesFile, valuesContents)
    Seq(chartFile, valuesFile)
  }

// Helper method to generate real paths from configured dependencies
// To be used for bundling external dependencies in docker file
def dependenciesToPaths(dependencies: Seq[ModuleID]): Seq[(String, String, String, String)] = {
  dependencies.map(module => {
    val parts = module.toString.split(""":""")
    val orgDir = parts(0).replaceAll("""\.""", """/""")
    val moduleName = parts(1).replaceAll("""\.""", """/""")
    val version = parts(2)
    val jarFile = moduleName + "-" + version + ".jar"
    (orgDir, moduleName, version, jarFile)
  })
}

// Custom actions to be called from sbt. Used in Makefile
lazy val showVersion = taskKey[Unit]("Show version")
showVersion := {
  println((version in ThisBuild).value)
}

lazy val showTeam = taskKey[Unit]("Show team")
showTeam := {
  println(team)
}

// Internal tasks
lazy val getImageName = taskKey[Option[String]]("Get image name")
lazy val getArtifactName = taskKey[String]("Get artifact name")
lazy val getChartName = taskKey[String]("Get chart name")

// External tasks
lazy val showImageName = taskKey[Unit]("Show image name")
lazy val showChartName = taskKey[Unit]("Show chart name")
lazy val listModules = taskKey[Unit]("List buildable modules")

// Logging
ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
