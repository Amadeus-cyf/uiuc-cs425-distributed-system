#!/bin/sh
# Define some constants
#ONSSERVER=mp1Out
# # PROJECT_PATH= 
# JAR_PATH=../$PROJECT_PATH
# BIN_PATH=$PROJECT_PATH/bin
# SRC_PATH= mp1

#IP_ADDR = enyij2@fa20-cs425-g53-02.cs.illinois.edu

# First remove the sources.list file if it exists and then create the sources file of the project
rm -f sources
find . -name '*.java' > sources.list
chmod 777 sources.list
rm -rf ../../out
mkdir ../../out
 
# Compile the project
javac -d ../../out -classpath ../../lib/json-20140107.jar @sources.list

# Run the project
java -classpath ../../out:../../lib/json-20140107.jar mp1.Server fa20-cs425-g53-02.cs.illinois.edu

