name := "algorithm thesis"

version := "0.1"

scalaVersion := "2.12.10"

fork in run := true

javaOptions in run += "-Djava.library.path=/home/ernesto/opencv-3.4.1/build/lib"
scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
scalacOptions += "-Ylog-classpath"
libraryDependencies ++= Seq(
  "com.github.mpilquist" %% "simulacrum"     % "0.13.0",
  "org.scalaz"           %% "scalaz-core"    % "7.2.26",
  "org.scalaz"           %% "scalaz-zio" % "1.0-RC4",
  "dev.zio" %% "zio" % "1.0.0-RC17",
  "log4j" % "log4j" % "1.2.17",
  "com.github.pureconfig" %% "pureconfig" % "0.12.1",
)
