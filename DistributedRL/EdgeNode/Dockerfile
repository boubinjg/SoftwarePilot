FROM ubuntu:18.04
#author information
MAINTAINER Jayson Boubin
LABEL version="1.0"
LABEL description="SoftwarePilot DRL Edge Node Dockerfile"

#Update Sources
RUN apt-get update

#Install basic applications
RUN apt-get install -y git   && \
    apt-get install -y vim   && \
    apt-get install -y cmake && \
    apt-get install -y curl  && \
    apt-get install -y unzip && \
    apt-get install -y nano && \
    apt-get install -y wget
#Install python3
RUN apt-get install -y python3.6 && \
    apt-get install -y python3-pip 

#install DLIB and face_recognition
RUN pip3 install face_recognition

#install OpenCV
RUN pip3 install opencv-python && \
    apt-get install -y libfontconfig1 libxrender1 && \
    apt-get install -y libsm6 libxext6
    
#install pydarknet for CPU
RUN apt-get install -y pkg-config && \
    pip3 install yolo34py
    
#install java8
#RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && apt-get update &&   apt-get install -y software-properties-common &&  add-apt-repository -y ppa:webupd8team/java &&   apt-get update &&   apt-get install -y oracle-java8-installer  &&   rm -rf /var/lib/apt/lists/* &&   rm -rf /var/cache/oracle-jdk8-installer

RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

RUN apt-get update && \
    apt-get install -y ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f


#Set Android Environment Variables
ENV SDK_URL="https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip" \
    ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_VERSION=26 \
    ANDROID_BUILD_TOOLS_VERSION=26.0.2

#Install Gradle
RUN cd /usr/local && \
    curl -L https://services.gradle.org/distributions/gradle-4.4-all.zip -o gradle-4.4-all.zip && \
    unzip gradle-4.4-all.zip && \
    rm gradle-4.4-all.zip

#Download Android SDK
RUN mkdir "$ANDROID_HOME" .android && \
    cd "$ANDROID_HOME"             && \
    curl -o sdk.zip $SDK_URL       && \
    unzip sdk.zip                  && \
    rm sdk.zip                     && \
    yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses

#Install Android Build Tool and Libraries
RUN $ANDROID_HOME/tools/bin/sdkmanager --update && \
    $ANDROID_HOME/tools/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" && \
    $ANDROID_HOME/tools/bin/sdkmanager "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2"

#Install Codebase
RUN cd /home && \
    git clone https://github.com/boubinjg/SoftwarePilot.git

#Set General environment variables
ENV LC_ALL=C.UTF-8 \
    LANG=C.UTF-8 \ 
    GRADLE_HOME=/usr/local/gradle-4.4/bin \
    AUAVHOME=/home/SoftwarePilot
#Fix Path Variable 
ENV PATH=$PATH:$GRADLE_HOME \ JAVA_HOME=/usr/lib/jvm/java-8-oracle

#Expose ports
EXPOSE 12013
EXPOSE 45045
EXPOSE 5117

RUN apt-get update && \
    apt-get install -y iputils-ping && \
    apt-get install -y net-tools
