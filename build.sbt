name := "blue-sky"

version := "0.1"

scalaVersion := "2.12.10"

fork in run := true

javaOptions in run += "-Djava.library.path=/home/ernestochero/opencv_build/opencv/build/lib"
scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
resolvers += Resolver.bintrayIvyRepo("com.eed3si9n", "sbt-plugins")
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC18-1",
  "com.github.pureconfig" %% "pureconfig" % "0.12.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.log4s" %% "log4s" % "1.8.2",
)
