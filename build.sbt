name := """akka-csv-script"""

organization := "ch.taggiasco"

version := "0.0.1"

scalaVersion := "2.12.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion     = "2.5.19"
  val alpakkaVersion  = "1.0-M1"
  val akkaHttpVersion = "10.1.5"

  Seq(
    "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % alpakkaVersion,
    "com.typesafe.akka"  %% "akka-http-core"          % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-http"               % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-slf4j"              % akkaVersion,
    "ch.qos.logback"     %  "logback-classic"         % "1.2.3",
    "org.scalatest"      %% "scalatest"               % "3.0.1"     % "test",
    "com.typesafe.akka"  %% "akka-stream-testkit"     % akkaVersion
  )
}
