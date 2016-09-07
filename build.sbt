organization := "io.github.nthportal"
name := "rest-paste-service"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "commons-io" % "commons-io" % "2.5",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.xerial" % "sqlite-jdbc" % "3.8.11.2"
)

