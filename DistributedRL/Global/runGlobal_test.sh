sudo docker run --rm -v /home/boubin/SoftwarePilot/DistributedRL/Data:/home/mydata:Z -v /home/boubin/Images/:/home/imageData:Z --name $1 --network "host" global /bin/bash -c "bash run.sh"
