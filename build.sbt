name := "tweet-moderation"
version := "0.1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.3",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Fork the JVM 
fork := true

javaOptions ++= sys.env.get("GROK_API_KEY").map(key => s"-DGROK_API_KEY=$key").toSeq

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/" 