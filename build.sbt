name := "blue-sky"

version := "0.1"

scalaVersion := "2.12.10"

fork in run := true

javaOptions in run += "-Djava.library.path=/home/ernestochero/opencv_build/opencv/build/lib"
scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
resolvers += Resolver.bintrayIvyRepo("com.eed3si9n", "sbt-plugins")
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC17",
  "log4j" % "log4j" % "1.2.17",
  "com.github.pureconfig" %% "pureconfig" % "0.12.1",
)
