import sys
import csv
from sklearn.neighbors import NearestNeighbors
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

knndata = None

def readInData():
    global knndata
    knndata = np.genfromtxt(sys.argv[1], delimiter=',')

def getCount():
    counts = []

    dirs = [x[0] for x in os.walk("../mydata")]
    jobs = len(dirs[1:])
    for i in dirs[1:]:
        count = len(glob.glob1(i, "*.csv"))
        counts.append(count)

    return [min(counts),jobs]

def checkForFiles(jobs, count):
    found = 0
    sn = os.environ['SERVERNUM']
    while(found < jobs):
        found = 0
        for i in range(0,jobs):
            out = os.popen('hadoop fs -test -e hdfs://127.0.0.1:9000/worker'+
                            str(sn)+'_'+str(i)+'/run_'+str(count)+' && echo $?').read()
            if(int(out) == 0):
                found += 1
                print("Server "+str(sn) + " Worker "+str(i)+" Job "+str(count) + " Complete")
    return True

def download(jobs, count):
    try:
        rmtree('tmp')
    except: 
        pass
    os.mkdir('tmp')
    sn=os.environ['SERVERNUM']
    for wn in range(jobs):
        cmd = 'hadoop fs -get hdfs://127.0.0.1:9000/worker'+str(sn)+'_'+str(wn)+'/run_'+str(count)+' tmp/'+str(wn)
        out = os.popen(cmd).read()
        print(out)


def rebuild():
    return 0;

download(2,0)

'''
readInData()

runs,jobs = getCount()
count = 0

while(count <= runs):
    print(count)
    checkForFiles(jobs, count)
    #Download all files from each dir into a temp diretcory
    download();
    #rebuild dataset
    rebuild();
    #upload dataset to HDFS
    upload();
    count += 1
'''
