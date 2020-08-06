sudo docker rm -f worker1_1
sudo docker rm -f worker0_1
sudo docker rm -f worker1_0
sudo docker rm -f worker0_0
sudo docker rm -f Aggregator0000
sudo docker rm -f Aggregator0001
sudo docker rm -f Aggregator0010
sudo docker rm -f Aggregator0011
sudo docker rm -f Aggregator0100
sudo docker rm -f Aggregator0101
sudo docker rm -f Aggregator0110
sudo docker rm -f Aggregator0111
sudo docker rm -f Aggregator1000
sudo docker rm -f Aggregator1001
sudo docker rm -f Aggregator1010
sudo docker rm -f Aggregator1011
sudo docker rm -f Aggregator1100
sudo docker rm -f Aggregator1101
sudo docker rm -f Aggregator1110
sudo docker rm -f Aggregator1111
sudo docker rm -f global
hadoop dfsadmin -safemode leave
hadoop fs -rm -r hdfs://10.0.0.6:9000/*
sudo rm -f ../Data/Worker*/run_*
