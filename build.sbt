import NativePackagerKeys._

packageArchetype.java_application

name := """HashTagCrawler"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.7.0",
  "io.reactivex" % "rxscala_2.11" % "0.25.0",
  "org.twitter4j" % "twitter4j-stream" % "4.0.3",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.h2database" % "h2" % "1.3.175",
  "org.json4s" %% "json4s-native" % "3.2.11"
)
