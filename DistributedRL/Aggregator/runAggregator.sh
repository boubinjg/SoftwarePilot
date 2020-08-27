sudo docker run -it --network "host" -e HDFS=$1 -e Models=0001 aggregator /bin/bash
