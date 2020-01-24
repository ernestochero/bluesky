#!/usr/bin/env bash
echo "Running sbt assembly to create a jar package"
sbt assembly
for i in {1..2}
do
echo "Running blueSky $i time"
java -Djava.library.path="/home/ernestochero/opencv_build/opencv/build/lib" -jar target/scala-2.12/blue-sky-assembly-0.1.jar
done
