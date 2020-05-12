docker rm -f worker1_1
docker rm -f worker0_1
docker rm -f worker1_0
docker rm -f worker0_0
docker rm -f server1
docker rm -f server0
hadoop fs -rm -r hdfs://127.0.0.1:9000/*
