sudo docker run -v /home/jayson/Github/SoftwarePilot/DistributedRL/Data/:/home/mydata:Z -it --network "host" -e HDFS=$1 -e Models=0001 aggregator /bin/bash
