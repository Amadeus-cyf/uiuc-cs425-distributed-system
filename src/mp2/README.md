# MP2 Simple Distributed File System (SDFS)

## Introduction

This is a simple distributed file system that allows you to put, get, delete, check the local as well as sdfs files on several servers, from any one of those machines. 

## High-level Design

- The introducer also serves as the master, which is responsible for handling all queries from any server in their **arriving sequences**. 

- For each put/delete operation, the system will store **four** replicas on four servers (because at most three failures at the same time). 

## Setup and Run all Servers

Go to `mp2` directory and execute `bash run.sh`. To successfully compile and run the program, 
you need to change the hostname in `run.sh` and enter the following command in terminal. This file will also generate 15 random files (3MB) indexed from 1 to 15, 
a small.txt (3MB), a large.txt (500MB).

- For Master server, change `mp2.Server` to `mp2.Master`in the last line
of file `run.sh` (we assume vm `fa20-cs425-g53-01` to be the default master).

- For non-Master servers, change the argument to the specific hostname in the last line of file `run.sh`. 

```
$ cd src/mp2
$ bash run.sh
```

## SDFS Implemented Commands

To **put** a local file to the SDFS which creates four replicas, follow the format:
```
put localfilename sdfsfilename
```

To **get** a sdfs file to the local, follow the format:
```
get sdfsfilename localfilename
```

To **delete** a sdfs file (all its replicas) in the SDFS, follow the format:
```
delete sdfsfilename
```

To **list** all servers that store a certain sdfs file, follow the format:
```
ls sdfsfilename
```

To **check the storage info** on a certain server:
```
storage
```

To **display** a sdfs/local file on the terminal:
```
more filepath (e.g., more local/small.txt)
```

To **compare** the sizes of two files (either in local or sdfs), follow the format:
```
compare filepath1 filepath2 (e.g., compare sdfs/1.txt local/1.txt)
```

To **see the differences** between two files (either in local or sdfs), follow the format:
```
diff filepath1 filepath2 (e.g., diff sdfs/1.txt local/1.txt)
```

To **clear** the currect terminal running interface:
```
clear
```

To print **membership** list that have joined the system:
```
print membership
```

To **leave** the system:
```
leave
```

To **rejoin** the system:
```
rejoin
```

To **switch** the heartbeating mode of the system (all-to-all or gossip-style):
```
switch mode
```

## Evaluation

We performed put/get evaluation on up-to 10 machines with self-generated datsets as well as Wikipedia raw corpus. Please see the report for more details.

## Contact

- Yifan Cao (yifanc7@illinois.edu)
- Enyi Jiang (enyij2@illinois.edu)
