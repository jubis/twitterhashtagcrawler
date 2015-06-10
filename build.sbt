import NativePackagerKeys._

packageArchetype.java_application

name := """HashTagCrawler"""

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.7.0"
)
