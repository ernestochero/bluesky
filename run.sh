#!/usr/bin/env bash
for i in {1..10}
do
echo "Running blueSky $i time"
java -Djava.library.path="/home/ernestochero/opencv-3.4.1/build/lib" -jar target/scala-2.12/BlueSky-assembly-0.1.jar
done
