import akka.sbt.AkkaKernelPlugin

name := "rabbitmq-akka-stream"

version := "2.0"

organization := "io.scalac"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.11"
  val akkaStreamVersion = "1.0-RC3"
  Seq(
    "com.typesafe.akka"          %%  "akka-actor"               % akkaVersion,
    "com.typesafe.akka"          %%  "akka-kernel"              % akkaVersion,
    "com.typesafe.akka"          %%  "akka-stream-experimental" % akkaStreamVersion,
    "io.scalac"                  %%  "reactive-rabbit"          % "1.0.0",
    "com.typesafe.scala-logging" %%  "scala-logging-slf4j"      % "2.1.2",
    "ch.qos.logback"             %   "logback-core"             % "1.1.2",
    "ch.qos.logback"             %   "logback-classic"          % "1.1.2",
    "org.scalatest"              %%  "scalatest"                % "2.2.1" % Test
  )
}

AkkaKernelPlugin.distSettings

distMainClass in Dist := "akka.kernel.Main io.scalac.rabbit.ConsumerBootable"