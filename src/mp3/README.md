# MP3 MapleJuice

## Introduction

This is a MapleJuice system which is similar to MapReduce. In this MP, we have set up MapJuice, write applications (Map
and Condorcet voting) for it, and compare its performance with Hadoop.

## High-level Design

- **Maple**: Split the input file, process each line, output intermediate file for each key.

- **Juice**: Read in the intermediate file, split according to the keys, process each part, output the file to the
  master (VM1).

## Setup and Run all Servers

Go to `mp3` directory and execute `bash dataset.sh` and then `bash build.sh`. `dataset.sh` will download two large files
from google drive as the inputs of MapleJuice. To successfully compile and run the program,
you need to change the hostname in `build.sh` and enter the following command in terminal.

- For Master server, change `mp2.Server` to `mp2.Master`in the last line
  of file `build.sh` (we assume vm `fa20-cs425-g53-01` to be the default master).

- For non-Master servers, change the argument to the specific hostname in the last line of file `build.sh`.

```
$ cd src/mp3
$ bash dataset.sh
$ bash build.sh
```

## MapleJuice Implemented Commands

Run the **Maple** stage, follow the below command. `<maple_exe>` can be
either `wordCount / building / votingCount / votingCompare` for our applications. `<num_maples` is the number
of the input file splits. `<sdfs_src_directory>` is the input file location. In our case, it could
be `../../building_NYC.txt`, `../../voting.txt`.

```
maple <maple_exe> <num_maples> <sdfs_intermediate_filename_prefix> <sdfs_src_directory>
```

Run the **Juice** stage, follow the below command. `<maple_exe>` can be
either `wordCount / building / votingCount / votingCompare` for our applications. `<num_maples` is the number
of the immediate file splits. `<sdfs_intermediate_filename_prefix>` is the prefix from Maple
stage. `<sdfs_dest_filename>` is the output file location. `delete_input`
is set to specify whether to delete the input files.

```
juice <juice_exe> <num_juices> <sdfs_intermediate_filename_prefix> <sdfs_dest_filename> delete_input={0,1}
```

Or you can run two stages together as the following command:

```
mapleJuice <maple_exe> <num_maples> <sdfs_intermediate_filename_prefix> <sdfs_src_directory> <juice_exe> <num_juices> <sdfs_dest_filename> delete_input={0,1}
```

To print **membership** list that have joined the system:

```
print membership
```

## Hadoop Running

### Start Master

VM6: `hdfs namenode -format` and `start-all.sh`.

Check if datanode started on VM6, VM7, VM8, VM9, VM10: `jps`

### Set up Input/Output Directories on Master

### Run MapReduce Application

**Word Count**: `hadoop jar MapReduce/wc1.jar VotingCount /home/enyij2/wc/input/test.txt /home/enyij2/wc/output`

**Condorcet Voting**: `hadoop jar MapReduce/vc1.jar VotingCount /home/enyij2/vc/input/voting.txt /home/enyij2/vc/output`

**
Building**:
`hadoop jar MapReduce/b.jar Building /home/enyij2/building/input/building_NYC.txt /home/enyij2/building/output`

### Check the Output

`hadoop fs -cat /home/enyij2/building/output/part-r-00000` (e.g. building)

### Remove Output Files

`hadoop fs -rm -r /home/enyij2/vc/output`(condorcet voting) and `hadoop fs -rm -r /home/enyij2/building/output`(
building).

## Contact

- Yifan Cao (yifanc7@illinois.edu)
- Enyi Jiang (enyij2@illinois.edu)
