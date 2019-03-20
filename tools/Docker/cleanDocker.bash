#!/bin/bash
#Clean up stopped containers and dead images to reclaim HDD space
sudo docker rm $(sudo docker ps -a -q -f status=exited -f status=dead)
sudo docker images | grep '^<none>' | awk '{print $3}' | xargs -n1 sudo docker rmi
