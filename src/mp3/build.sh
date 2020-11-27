#!/bin/sh
rm -f sources
find ../mp2/ServerInfo.java ../mp2/DataTransfer.java ../mp2/failureDetector ../mp2/constant ../mp2/message ../mp3 -name '*.java' > sources.list
chmod 777 sources.list
rm -rf ../../out
mkdir ../../out

# Compile the project
javac -d ../../out -classpath ../../lib/json-20140107.jar @sources.list

# Run the project
java -classpath ../../out:../../lib/json-20140107.jar mp3.Server fa20-cs425-g53-02.cs.illinois.edu

# java -classpath ../../out:../../lib/json-20140107.jar mp3.Master fa20-cs425-g53-01.cs.illinois.edu