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

# Clean the previous run of sdfs/local file directories
rm -rf ../../sdfs
mkdir ../../sdfs
rm -rf ../../local
mkdir ../../local

# Generate 15 random files with different sizes in local folder
for((i=0;i<=15;i++))
do
	dd if=/dev/urandom of=../../local/$i.txt bs=1000000 count=3
done

# Generate a samll file
dd if=/dev/urandom of=../../local/small.txt bs=1000000 count=3

# Generate a large file
dd if=/dev/urandom of=../../local/large.txt bs=1000000 count=500

# Compile the project
javac -d ../../out -classpath ../../json-20140107.jar @sources.list

# Run the project
java -classpath ../../out:../../json-20140107.jar mp2.Server fa20-cs425-g53-03.cs.illinois.edu
