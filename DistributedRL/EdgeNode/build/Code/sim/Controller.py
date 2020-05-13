import os, glob
import subprocess
from os import listdir
from os.path import isfile, join

def checkForUpdate(count, lastUpdate):
    sn = os.environ['SERVERNUM']
    updateCount = -1
    print([count, lastUpdate])
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
        os.remove('knndatasetGI')
        out = os.popen('/opt/hadoop/bin/hadoop fs -copyToLocal hdfs://127.0.0.1:9000/server_'+
              sn+'/run_'+str(updateCount)+'/knndata knndatasetGI').read()

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

for f in csvs:
    print("wow");
    lastUpdate = checkForUpdate(count, lastUpdate)
    
    subprocess.call(['bash','runGI.bash','/home/mydata/'+f,'10', str(count)])
    os.chdir('tmp/')

    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])

    for x in glob.glob("*.*"):
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put',x,'hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)+"/"+x])
        print(x)
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-touchz','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)+"/done"])
    os.chdir(cwd)
    
    count += 1

