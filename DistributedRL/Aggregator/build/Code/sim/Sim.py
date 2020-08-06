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
    try:
        knndata = np.genfromtxt(sys.argv[1], delimiter=',')
    except:
        knndata = []

def readIn(data):
    global knndata
    knndata = np.genfromtxt(data, delimiter=',')

def getCount():
    hdfs = os.environ['HDFS']
    models = os.environ['Models']
    for i in range(100):
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/model_'+
                            models+'/run_'+str(i)+'/knndata && echo $?').read()
        #print('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/model_'+
        #                    models+'/run_'+str(i)+'/knndata && echo $?')
        try:
            if(int(out) == 0):
                print("Model step "+str(i)+" has been run")
        except:
            print("Model step "+str(i)+" has not run")
            return i
    return 100

def checkForFiles(jobs, count):
    found = 0
    hdfs = os.environ['HDFS']
    while(found < len(jobs)):
        found = 0
        foundNums = []
        for i in jobs:
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/worker'+
                            str(i[0])+'_'+str(i[1])+'/run_'+str(count)+'/done && echo $?').read()
            try:
                if(int(out) == 0 and i not in foundNums):
                    found += 1
                    foundNums.append(i)
                    print("Server "+str(i[0]) + " Worker "+str(i[1])+" Job "+str(count) + " Complete")
            except:
                    print("Server "+str(i[0]) + " Worker "+str(i[1])+" Job "+str(count) + " incomplete")
        time.sleep(10)
    return True

def download(jobs, count):
    hdfs = os.environ['HDFS']
    try:
        rmtree('tmp')
    except: 
        pass
    os.mkdir('tmp')
    for i in jobs:
        cmd = '/opt/hadoop/bin/hadoop fs -get hdfs://'+hdfs+':9000/worker'+str(i[0])+'_'+str(i[1])+'/run_'+str(count)+' tmp/'+str(i[0])+str(i[1])
        out = os.popen(cmd).read()
        print(out)

def removeOldData(jobs, count):
    hdfs = os.environ['HDFS']
    
    for i in jobs:
        cmd = '/opt/hadoop/bin/hadoop fs -rm -r hdfs://'+hdfs+':9000/worker'+jobs[0]+'_'+jobs[1]+'/run_'+str(count)
        out = os.popen(cmd).read()
        print(out)

def rebuild(jobs, knndata):
    for i in jobs:
        jobBin = str(i[0])+str(i[1])
        pwd = os.getcwd()
        print(pwd)
        os.chdir(pwd+'/tmp/'+str(jobBin))
        for f in glob.glob('*'):
            print(f)
        for f in glob.glob('*.csv'):
            fname = f.split('.')[0]
            #cmd = 'bash '+pwd+'/Parser/runParser.sh '+pwd+'/tmp/'+str(i)+'/'+f
            #print(cmd)
            #out = os.popen(cmd).read().rstrip()[1:-1].split(',')
            
            #line = []
            #for feat in out:
            #    line.append(float(feat.split('=')[1]))
            
            data = []
            with open(pwd+'/tmp/'+str(jobBin)+'/'+fname+'.csv') as csvf:
                reader = csv.reader(csvf)
                data = list(reader)[0]
        
            line = []
            for d in data:
                line.append(float(d))

            #print(line)

            npl = np.asarray(line).reshape((1,17))
            
            #Replace repeats in dataset with new info?
            #print('WHOLE ARR')
            #print(npl)
            #print('PART')
            #print(npl[0][1:13])

            if knndata == []:
                knndata = npl
            else:
                knndata = np.concatenate((knndata, npl))
            
            print(npl.shape)
            print(knndata.shape)

        os.chdir(pwd)
    return knndata

def initCount():
    count = 0;
    HDFS=os.environ['HDFS']
    while(True):
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+HDFS+':9000/server_'+
                        str(sn)+'/run_'+str(count)+'/knndata && echo $?').read()
        try:
            if(int(out) == 0):
                print('Update Available: '+str(count))
                count += 1
            else:
                print('No New Update')
                return count
        except:
            print('No New Update: '+str(count))
            return count
    return 0

def upload(knndata, count):
    models = os.environ['Models']
    HDFS = os.environ['HDFS']
    np.savetxt("tmp/knndata",knndata,delimiter=',')
    if(count == 0):
        os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://'+HDFS+':9000/model_'+models).read()
    os.popen('/opt/hadoop/bin/hadoop fs -mkdir hdfs://'+HDFS+':9000/model_'+models+'/run_'+str(count)).read()
    os.popen('/opt/hadoop/bin/hadoop fs -put tmp/knndata hdfs://'+HDFS+':9000/model_'+models+'/run_'+str(count)).read()
    
    os.popen('/opt/hadoop/bin/hadoop fs -put -f tmp/knndata hdfs://'+HDFS+':9000/Models/'+models).read()


count = getCount()
runs = 100
models = os.environ['Models']
HDFS = os.environ['HDFS']
jobs = []
opts = {0: [0,0], 1: [0,1], 2: [1,0], 3: [1,1]}
for i in range(4):
    if(models[i] == '1'):
        jobs.append(opts[i])

print('Jobs Tracked: ')
print(jobs)

if(count == 0):
    readIn('/home/mydata/Worker0_0/knn1500') 
else:
    os.popen('/opt/hadoop/bin/hadoop fs -get hdfs://'+HDFS+':9000/model_'+
            models+'/run_'+str(count-1)+'/knndata .').read()

    readIn('knndata')

print(len(knndata))

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
    #remove build dataset
    #removeOldData(jobs, count)
    count += 1

