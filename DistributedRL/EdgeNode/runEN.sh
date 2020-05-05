sudo docker run -v /home/boubin/SoftwarePilot/DistributedRL/Data/Worker0_0:/home/mydata:Z -v /home/boubin/Images/:/home/imageData:Z -it --network "host" -e SERVERNUM=0 -e WORKERNUM=0 spen /bin/bash
