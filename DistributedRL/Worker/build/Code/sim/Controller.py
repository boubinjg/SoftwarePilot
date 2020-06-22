import os, glob
import subprocess
from os import listdir
from os.path import isfile, join
import time
import traceback
import shutil
import numpy as np
import csv


def checkForUpdate(count, lastUpdate):
    sn = os.environ['SERVERNUM']
    updateCount = -1
    print("UPDATE CHECK: SERVER")
    print([count, lastUpdate])
    while(updateCount == -1):
        time.sleep(10)
        print('Checking')
        for i in range(lastUpdate+1, count):
            print([i,count])
            print("In Check "+str(i))
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/server_'+
                  sn+'/run_'+str(i)+'/knndata && echo $?').read()
            try:
                if(int(out) == 0):
                    updateCount = i
                    print("Update Ready: "+str(i))
            except:
                traceback.print_exc()
                print("Update Not Ready")
                break;
    if(updateCount > -1):
        try:
            os.remove('knndatasetGI')
        except:
            pass
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://127.0.0.1:9000/server_'+
              sn+'/run_'+str(updateCount)+'/knndata knndatasetGI').read()

    return updateCount

def updateLocal():
        pwd = os.getcwd()
        print(pwd)
        
        knndata = np.genfromtxt('knndatasetGI',delimiter=',')

        os.chdir(pwd+'/tmp/')
        for f in glob.glob('*'):
            print(f)
        for f in glob.glob('*.JPG'):
            fname = f.split('.')[0]
            #cmd = 'bash '+pwd+'/Parser/runParser.sh '+pwd+'/tmp/'+str(i)+'/'+f
            #print(cmd)
            #out = os.popen(cmd).read().rstrip()[1:-1].split(',')

            #line = []
            #for feat in out:
            #    line.append(float(feat.split('=')[1]))

            data = []
            with open(pwd+'/tmp/'+fname+'.csv') as csvf:
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

        np.savetxt("knndatasetGI",knndata,delimiter=',')

def getCount():
    sn = os.environ['SERVERNUM']
    updateCount = -1
    check = 0
    while(True):
        print("Looking for update: "+str(check))
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/server_'+
                  sn+'/run_'+str(check)+'/knndata && echo $?').read()
        try:
            if(int(out) == 0):
                updateCount = check
                check += 1
            else:
                print('No Update Ready: '+str(check))
                return updateCount
        except:
            print('No Update Found: '+str(check))
            traceback.print_exc()
            return updateCount
    return -1

def getCountGlobal():
    updateCount = -1
    check = 0
    while(True):
        print("Looking for update: "+str(check))
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/global/run_'+str(check)+'/knndata && echo $?').read()
        try:
            if(int(out) == 0):
                updateCount = check
                check += 1
            else:
                print('No Update Ready: '+str(check))
                return updateCount
        except:
            print('No Update Found: '+str(check))
            traceback.print_exc()
            return updateCount
    return -1


def checkForGlobalUpdate(count, lastUpdate):
    updateCount = -1
    print([count, lastUpdate])
    print("UPDATE CHECK: GLOBAL")
    while(updateCount == -1):
        time.sleep(10)
        for i in range(lastUpdate+1, count):
            print("In Check GLOBAL"+str(i))
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/global/run_'+str(i)+'/knndata && echo $?').read()
            try:
                if(int(out) == 0):
                    updateCount = i
                    print("Update Ready: "+str(i))
            except:
                print('Update Not Ready')
                break;
    
    if(updateCount > -1):
        try:
            os.remove('knndatasetGI')
        except:
            pass
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://127.0.0.1:9000/global'+
              '/run_'+str(updateCount)+'/knndata knndatasetGI').read()
    return updateCount

cwd = os.getcwd()
csvs = []
os.chdir("/home/mydata/")
for f in glob.glob("*.csv"):
    csvs.append(f)

os.chdir(cwd)

sn=os.environ["SERVERNUM"]
wn=os.environ["WORKERNUM"]
    
try:
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir', 'hdfs://127.0.0.1:9000/worker'+sn+'_'+wn])
except Exception as e:
    print(e)

#lastUpdate = getCount()
#lastUpdate = getCountGlobal()
lastUpdate = -1
count = 0

print([lastUpdate, count])

for rangeCounter in range(1,21):
    DSVal = rangeCounter*500
    shutil.copyfile('/home/mydata/knn'+str(DSVal),'/home/sim/knndatasetGI')
    for f in csvs:
        if(count != 0):
            print("Checking for Server Update")
            #time.sleep(300)
            #lastUpdate = checkForUpdate(count, lastUpdate)
            #lastUpdate = checkForGlobalUpdate(count, lastUpdate)
            updateLocal()

        subprocess.call(['bash','runGI.bash','/home/mydata/'+f, '60', str(100*rangeCounter)])
        os.chdir('tmp/')

        #subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])

        nImgs = len(glob.glob("*.JPG"))
    
        f = open('/home/mydata/run_'+str(count), 'w+')
        f.truncate(0)
        f.write('len = '+str(nImgs)+'\n')
        energy = open('/home/sim/tmp/energy','r+')
        f.write(energy.read()+'\n')
        energy.close()
        f.close()

        os.remove('/home/sim/tmp/energy')

        #for x in glob.glob("*.*"):
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/tmp/','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/'])
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mv','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/tmp','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])
   
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-touchz','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)+"/done"])
        os.chdir(cwd)
    
        count += 1
