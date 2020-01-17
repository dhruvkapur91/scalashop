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

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")
