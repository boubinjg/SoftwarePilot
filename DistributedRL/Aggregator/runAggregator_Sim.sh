sudo docker run -d -v /home/jayson/Github/SoftwarePilot/DistributedRL/Data/:/home/mydata:Z --name $2 --network "host" -e HDFS=$3 -e Models=$1 aggregator /bin/bash -c "bash run.sh"
