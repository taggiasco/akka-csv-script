name := """akka-csv-script"""

organization := "ch.taggiasco"

version := "0.0.1"

scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion     = "2.5.14"
  val alpakkaVersion  = "0.20"
  val akkaHttpVersion = "10.1.4"

  Seq(
    "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % alpakkaVersion,
    "com.typesafe.akka"  %% "akka-http-core"          % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-http"               % akkaHttpVersion,
    "org.scalatest"      %% "scalatest"               % "3.0.1"     % "test",
    "com.typesafe.akka"  %% "akka-stream-testkit"     % akkaVersion
  )
}
