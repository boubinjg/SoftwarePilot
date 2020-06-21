sudo docker rm -f worker1_1
sudo docker rm -f worker0_1
sudo docker rm -f worker1_0
sudo docker rm -f worker0_0
sudo docker rm -f server1
sudo docker rm -f server0
sudo docker rm -f global
hadoop dfsadmin -safemode leave
hadoop fs -rm -r hdfs://127.0.0.1:9000/*
sudo rm -f ../Data/Worker*/run_*
