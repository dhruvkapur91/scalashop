course := "parprog1"
assignment := "scalashop"

scalaVersion := "2.13.0"
scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.storm-enroute" %% "scalameter-core" % "0.19",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
  "org.scalaz" %% "scalaz-core" % "7.2.28",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "com.novocode" % "junit-interface" % "0.11" % Test
)
libraryDependencies += "org.scalanlp" %% "breeze" % "1.0"
libraryDependencies += "org.json4s" %% "json4s-core" % "3.7.0-M2"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.7.0-M2"
libraryDependencies += "org.json4s" %% "json4s-ext" % "3.7.0-M2"

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")
