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
        foundNums = []
        for i in range(0,jobs):
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/worker'+
                            str(sn)+'_'+str(i)+'/run_'+str(count)+'/done && echo $?').read()
            try:
                if(int(out) == 0 and i not in foundNums):
                    found += 1
                    foundNums.append(i)
                    print("Server "+str(sn) + " Worker "+str(i)+" Job "+str(count) + " Complete")
            except:
                    print("Server "+str(sn) + " Worker "+str(i)+" Job "+str(count) + " incomplete")
        time.sleep(10)
    return True

def download(jobs, count):
    try:
        rmtree('tmp')
    except: 
        pass
    os.mkdir('tmp')
    sn=os.environ['SERVERNUM']
    for wn in range(jobs):
        cmd = '/opt/hadoop/bin/hadoop fs -get hdfs://127.0.0.1:9000/worker'+str(sn)+'_'+str(wn)+'/run_'+str(count)+' tmp/'+str(wn)
        out = os.popen(cmd).read()
        print(out)

def rebuild(jobs, knndata):
    for i in range(jobs):
        pwd = os.getcwd()
        print(pwd)
        os.chdir(pwd+'/tmp/'+str(i))
        for f in glob.glob('*'):
            print(f)
        for f in glob.glob('*.JPG'):
            fname = f.split('.')[0]
            cmd = 'bash '+pwd+'/Parser/runParser.sh '+pwd+'/tmp/'+str(i)+'/'+f
            print(cmd)
            out = os.popen(cmd).read().rstrip()[1:-1].split(',')
            
            line = []
            for feat in out:
                line.append(float(feat.split('=')[1]))
        
            data = []
            with open(pwd+'/tmp/'+str(i)+'/'+fname+'.csv') as csvf:
                reader = csv.reader(csvf)
                data = list(reader)[0]
        
            for d in data:
                line.append(float(d))

            print(line)
		
            npl = np.asarray(line).reshape((1,17))
            knndata = np.concatenate((knndata, npl))
            print(npl.shape)
            print(knndata.shape)

        os.chdir(pwd)
    return knndata

def upload(knndata, count):
    sn = os.environ['SERVERNUM']
    np.savetxt("tmp/knndata",knndata,delimiter=',')
    if(count == 0):
        os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://127.0.0.1:9000/server_'+sn).read()
    os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://127.0.0.1:9000/server_'+sn+'/run_'+str(count)).read()
    os.popen('/opt/hadoop/bin/hadoop fs -put tmp/knndata hdfs://127.0.0.1:9000/server_'+sn+'/run_'+str(count)).read()

readInData()

runs,jobs = getCount()
count = 0

print(runs)
while(count < runs):
    print(count)
    checkForFiles(jobs, count)
    #Download all files from each dir into a temp diretcory
    download(jobs, count);
    #rebuild dataset
    knndata = rebuild(jobs, knndata);
    #print(knndata.shape)
    #upload dataset to HDFS
    upload(knndata, count);
    count += 1

