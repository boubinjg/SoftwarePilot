sudo docker rm -f worker0_0
sudo docker rm -f worker0_1
sudo docker rm -f worker1_0
sudo docker rm -f worker1_1
kubectl delete --all pods
hadoop dfsadmin -safemode leave
hadoop fs -rm -r hdfs://184.57.4.230:9000/*
sudo rm -f ../Data/Worker*/run_*
sudo rm -rf tmp
