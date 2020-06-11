import sys
import csv
import numpy as np
import statistics
from collections import defaultdict
import copy
import os 
from shutil import copyfile
from shutil import rmtree
import glob
import subprocess
import os
import time

knndata = None

def readInData():
    global knndata
    knndata = np.genfromtxt(sys.argv[1], delimiter=',')

def getCount():
    counts = []

    dirs = [x[0] for x in os.walk("../mydata")]
    jobs = 0
    for i in dirs[1:]:
        if(i.split('/')[2][0:6] == 'Server' and len(i.split('/')) == 3):
            jobs+=1

    
    for i in dirs[1:]:
        tail = i.split('/')[2]
        if(tail[0:6] == "Worker" and tail[6] != "_"):
            count = len(glob.glob1(i, "*.csv"))
            counts.append(count)

    print(counts)
    return min(counts)*16, jobs

def checkForFiles(jobs, count):
    found = 0
    dirs = [x[0] for x in os.walk("../mydata/")]
   
    foundNums = []
    while(found < jobs):
        for i in dirs[1:]:
            if(i.split('/')[2][0:6] == "Server" and len(i.split('/')) == 3):
                sn = int(i.split('/')[2][6:])
                cmd = '/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/server_'+str(sn)+'/run_' + str(count) + '/knndata && echo $?'
                print(cmd)
                out = os.popen(cmd).read()
                try:
                    if(int(out) == 0 and sn not in foundNums):
                        found += 1
                        foundNums.append(sn)
                        print("Global:  Server "+str(i)+" Job "+str(count) + " Complete")
                except:
                    print("Global:  Server "+str(i)+" Job "+str(count) + " incomplete")
        time.sleep(10)
    return True

def download(jobs, count):
    try:
        rmtree('tmp')
    except: 
        pass
    os.mkdir('tmp')
    for sn in range(jobs):
        cmd = '/opt/hadoop/bin/hadoop fs -get hdfs://127.0.0.1:9000/server_'+str(sn)+'/run_'+str(count)+' tmp/'+str(sn)
        out = os.popen(cmd).read()
        print(out)

def rebuild(jobs, knndata, knnSize):
    for i in range(jobs):
        pwd = os.getcwd()
        os.chdir(pwd+'/tmp/'+str(i))

        knnDataTmp = np.genfromtxt('knndata', delimiter=',')
        print([len(knnDataTmp), len(knndata)])

        oldLen = knnSize[i]
        newLen = len(knnDataTmp)

        print("KNNshape")
        print(knndata.shape)
        print("tmpShape")
        print(knnDataTmp[i].shape)

        for j in range(oldLen, len(knnDataTmp)):
            knndata = np.concatenate((knndata, knnDataTmp[j].reshape((1,17))))
            print(i)

        knnSize[i] = newLen

        print(knndata.shape)

        os.chdir(pwd)
    return knndata, knnSize

def getKNNSize(knndata, jobs):
    ret = []
    for i in range(jobs):
        ret.append(len(knndata))

    return ret;

def upload(knndata, count):
    print("Uploading")
    np.savetxt("tmp/knndata",knndata,delimiter=',')
    if(count == 0):
        os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://127.0.0.1:9000/global').read()
    os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://127.0.0.1:9000/global/run_'+str(count)).read()
    os.popen('/opt/hadoop/bin/hadoop fs -put tmp/knndata hdfs://127.0.0.1:9000/global/run_'+str(count)).read()
    print("Upload Complete")

readInData()
runs, jobs = getCount()
print((runs, jobs))

knnSize = getKNNSize(knndata, jobs)


count = 0

while(count < runs):
    checkForFiles(jobs, count)
    #Download all files from each dir into a temp diretcory
    download(jobs, count);
    #rebuild dataset
    knndata, knnSize = rebuild(jobs, knndata, knnSize);
    #print(knndata.shape)
    #upload dataset to HDFS
    upload(knndata, count);
    count += 1
