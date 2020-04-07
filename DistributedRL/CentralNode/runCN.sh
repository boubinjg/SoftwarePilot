sudo docker run -v /CNData:/var/lib/cassandra -it -p 5117:5117/udp -p 12013:12013 -p 0.0.0.0:9042:9042 -p 0.0.0.0:9160:9160 --network "host" spcn /bin/bash 
