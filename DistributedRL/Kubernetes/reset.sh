kubectl delete --all pods
hadoop dfsadmin -safemode leave
hadoop fs -rm -r hdfs://10.0.0.6:9000/*
sudo rm -f ../Data/Worker*/run_*
