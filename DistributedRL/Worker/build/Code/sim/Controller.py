import os, glob
import subprocess
from os import listdir
from os.path import isfile, join
import time
import traceback
import shutil
import numpy as np
import csv
import time

def checkForUpdate(count, lastUpdate):
    sn = os.environ['SERVERNUM']
    hdfs = os.environ['HDFS']
    updateCount = -1
    print("UPDATE CHECK: SERVER")
    print([count, lastUpdate])
    while(updateCount == -1):
        time.sleep(10)
        print('Checking')
        for i in range(lastUpdate+1, count):
            print([i,count])
            print("In Check "+str(i))
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/server_'+
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
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://'+hdfs+':9000/server_'+
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
    hdfs = os.environ['HDFS']
    updateCount = -1
    check = 0
    while(True):
        print("Looking for update: "+str(check))
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/server_'+
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
    hdfs = os.environ['HDFS']

    while(True):
        print("Looking for update: "+str(check))
        out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/global/run_'+str(check)+'/knndata && echo $?').read()
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
    hdfs = os.environ['HDFS']
    while(updateCount == -1):
        time.sleep(10)
        for i in range(lastUpdate+1, count):
            print("In Check GLOBAL"+str(i))
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://'+hdfs+':9000/global/run_'+str(i)+'/knndata && echo $?').read()
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
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://'+hdfs+':9000/global'+
              '/run_'+str(updateCount)+'/knndata knndatasetGI').read()
    return updateCount

def get_size(start_path):
    total_size = 0
    for dirpath, dirnames, filenames in os.walk(start_path):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            # skip if it is symbolic link
            if not os.path.islink(fp):
                total_size += os.path.getsize(fp)
    return total_size

cwd = os.getcwd()
csvs = []
os.chdir('/home/mydata/Worker'+sn+'_'+wn+'/')
for f in glob.glob("*.csv"):
    csvs.append(f)

os.chdir(cwd)

sn=os.environ["SERVERNUM"]
wn=os.environ["WORKERNUM"]
hdfs = os.environ['HDFS']

try:
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir', 'hdfs://'+hdfs+':9000/worker'+sn+'_'+wn])
except Exception as e:
    print(e)

#lastUpdate = getCount()
#lastUpdate = getCountGlobal()
lastUpdate = -1
count = 0

print([lastUpdate, count])

start = time.time()

DSVal = 1500  
shutil.copyfile('/home/mydata/Worker'+sn+'_'+wn+'/knn'+str(DSVal),'/home/sim/knndatasetGI')

for rangeCounter in range(1,2):
    hdfs = os.environ['HDFS']
    #DSVal = rangeCounter*500
    shutil.copyfile('/home/mydata/Worker'+sn+'_'+wn+'/knn'+str(DSVal),'/home/sim/knndatasetGI')
    for f in csvs:
        if(count != 0):
            print("Checking for Server Update")
            #time.sleep(300)
            lastUpdate = checkForUpdate(count, lastUpdate)
            #lastUpdate = checkForGlobalUpdate(count, lastUpdate)
            #updateLocal()

        subprocess.call(['bash','runNet.bash','/home/mydata/Worker'+sn+'_'+wn+'/'+f, '80', str(100),'5'])
        os.chdir('tmp/')

        #subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])

        nImgs = len(glob.glob("*.JPG"))
    
        f = open('/home/mydata/Worker'+sn+'_'+wn+'/run_'+str(count), 'w+')
        f.truncate(0)
        f.write('len = '+str(nImgs)+'\n')
        energy = open('/home/sim/tmp/energy','r+')
        
        contents = energy.read()

        tput = float(contents.split()[7]) * (1024*1024)

        transferSize = get_size('/home/sim/tmp')
        transferTime = transferSize/tput
        print(transferTime)
        #time.sleep(transferTime)
        contents += 'Transfer Time: '+str(transferTime)+'\n'

        f.write(contents+'\n')       
        
        print('Transfer Size: '+str(transferSize))

        energy.close()
        f.close()

        os.remove('/home/sim/tmp/energy')

        #for x in glob.glob("*.*"):
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/tmp/','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/'])
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mv','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/tmp','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])

        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-touchz','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/run_'+str(count)+"/done"])
        os.chdir(cwd)
    
        
        end = time.time()
        print("Time so far: "+str(end-start))
        count += 1

end = time.time()
print(end-start)

f = open('/home/sim/tmp/runtime','w+')
f.write('Total Time: '+str(end-start)+'\n')
f.close()

subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/tmp/runtime','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/'])
