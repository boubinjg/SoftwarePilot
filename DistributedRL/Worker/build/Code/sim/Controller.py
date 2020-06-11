import os, glob
import subprocess
from os import listdir
from os.path import isfile, join
import time

def checkForUpdate(count, lastUpdate):
    sn = os.environ['SERVERNUM']
    updateCount = -1
    print("UPDATE CHECK: SERVER")
    print([count, lastUpdate])
    while(updateCount == -1):
        time.sleep(10)
        for i in range(lastUpdate+1, count):
            print("In Check "+str(i))
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://127.0.0.1:9000/server_'+
                  sn+'/run_'+str(i)+'/knndata && echo $?').read()
            try:
                if(int(out) == 0):
                    updateCount = i
                    print("Update Ready: "+str(i))
            except:
                break;
                print("Update Not Ready")
    
    if(updateCount > -1):
        try:
            os.remove('knndatasetGI')
        except:
            pass
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://127.0.0.1:9000/server_'+
              sn+'/run_'+str(updateCount)+'/knndata knndatasetGI').read()

    return updateCount

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
                break;
        print("Update Not Ready")
    
    if(updateCount > -1):
        os.remove('knndatasetGI')
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

count = 0
lastUpdate = -1
lastUpdateGlobal = -1

for rangeCounter in range(0,16):
    for f in csvs:
        if(count != 0):
            print("Checking for Server Update")
            #time.sleep(300)
            #lastUpdate = checkForUpdate(count, lastUpdate)
            lastUpdateGlobal = checkForGlobalUpdate(count, lastUpdateGlobal)
 
        subprocess.call(['bash','runGI.bash','/home/mydata/'+f, '60', '30', str(count)])
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

