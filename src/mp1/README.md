### MP1 Failure Detector
To compile and run the program, change the hostname in run.sh and enter the following command in terminal,
```shell script
bash run.sh
```
This will start a server and let it join the system.

To print membership list,
```shell script
print membership
```

There are two kinds of failure detector mode: Gossip and All-to-All. To switch mode,
```shell script
swtich mode
```

To leave the system,
```shell script
leave
```

To rejoin the system,
```shell script
rejoin
```
