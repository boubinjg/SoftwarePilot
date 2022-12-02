FROM ubuntu:20.04

RUN apt-get update

RUN apt-get update &&\
    apt-get install -y software-properties-common &&\
    add-apt-repository ppa:deadsnakes/ppa &&\
    apt-get install -y tzdata &&\
    apt-get install -y python3.8 &&\
    apt-get install -y python3-pip

WORKDIR /drone-api

COPY requirements.txt .

RUN python3 -m pip install --upgrade pip

RUN pip install -r requirements.txt

RUN pip install https://github.com/Parrot-Developers/olympe/releases/download/v7.3.0/parrot_olympe-7.3.0-py3-none-manylinux_2_27_x86_64.whl

COPY ./app ./app

RUN apt-get install -y build-essential yasm cmake libtool libc6 libc6-dev \
   unzip freeglut3-dev libglfw3 libglfw3-dev libjson-c-dev libcurl4-gnutls-dev \
   libgles2-mesa-dev

CMD ["python3", "./app/main.py"]
