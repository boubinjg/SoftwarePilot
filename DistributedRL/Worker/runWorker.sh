sudo docker run --rm -v /home/jayson/Github/SoftwarePilot/DistributedRL/Data/Worker$1_$2:/home/mydata:Z -it --network "host" -e SERVERNUM=$1 -e WORKERNUM=$1 -e HDFS=$3 worker /bin/bash
