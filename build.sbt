name := "algorithm thesis"

version := "0.1"

scalaVersion := "2.12.6"

fork in run := true

javaOptions in run += "-Djava.library.path=/home/ernestochero/opencv-3.4.1/build/lib"

libraryDependencies ++= Seq(
"com.github.mpilquist" %% "simulacrum"     % "0.13.0",
"org.scalaz"           %% "scalaz-core"    % "7.2.26",
"org.scalaz"           %% "scalaz-zio" % "1.0-RC4",
)