#!/bin/sh
# Define some constants
#ONSSERVER=mp1Out
# # PROJECT_PATH= 
# JAR_PATH=../$PROJECT_PATH
# BIN_PATH=$PROJECT_PATH/bin
# SRC_PATH= mp1

# First remove the sources.list file if it exists and then create the sources file of the project
rm -f sources
find . -name '*.java' > sources.list
chmod 777 sources.list
 
# Compile the project
javac -d ../../out -classpath ../../json-20140107.jar @sources.list

# Run the project
java -classpath ../../out:../../json-20140107.jar mp1.Server

