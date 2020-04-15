FROM ubuntu:18.04
#author information
MAINTAINER Jayson Boubin
LABEL version="1.0"
LABEL description="SoftwarePilot DRL Central Node Dockerfile"

ENV HADOOP_HOME /opt/hadoop
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

#Update Sources
RUN apt-get update

#Install basic applications
RUN apt-get update && apt-get install -y \
    git \
    vim \
    cmake \
    curl \
    unzip \
    nano \
    wget \
    rsync \
    ssh \
    openjdk-8-jdk
#Install python3
RUN apt-get install -y python3.6 && \
    apt-get install -y python3-pip 

#install java8
#RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && apt-get update &&   apt-get install -y software-properties-common &&  add-apt-repository -y ppa:webupd8team/java &&   apt-get update &&   apt-get install -y oracle-java8-installer  &&   rm -rf /var/lib/apt/lists/* &&   rm -rf /var/cache/oracle-jdk8-installer

# download and extract hadoop, set JAVA_HOME in hadoop-env.sh, update path
RUN \
  wget https://archive.apache.org/dist/hadoop/core/hadoop-2.7.3/hadoop-2.7.3.tar.gz && \
  tar -xzf hadoop-2.7.3.tar.gz && \
  mv hadoop-2.7.3 $HADOOP_HOME && \
  echo "export JAVA_HOME=$JAVA_HOME" >> $HADOOP_HOME/etc/hadoop/hadoop-env.sh && \
  echo "PATH=$PATH:$HADOOP_HOME/bin" >> ~/.bashrc
 
# create ssh keys
RUN \
  ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa && \
  cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys && \
  chmod 0600 ~/.ssh/authorized_keys
 
# copy hadoop configs
#ADD configs/*xml $HADOOP_HOME/etc/hadoop/
 
# copy ssh config
#ADD configs/ssh_config /root/.ssh/config
 
# copy script to start hadoop
#ADD start-hadoop.sh start-hadoop.sh
 
# expose various ports
EXPOSE 8088 50070 50075 50030 50060
 
# start hadoop
CMD bash start-hadoop.sh

RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

RUN apt-get update && \
    apt-get install -y ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f
VOLUME /var/lib/cassandra

RUN pip3 install setuptools
RUN pip3 install cassandra-driver

RUN mkdir /home/tools
COPY /Code/tools /home/tools

#Expose ports
EXPOSE 12013
EXPOSE 5117

RUN apt-get update && \
    apt-get install -y iputils-ping && \
    apt-get install -y net-tools
