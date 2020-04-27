import os, glob
import subprocess
from os import listdir
from os.path import isfile, join

cwd = os.getcwd()
csvs = []
os.chdir("/home/mydata/")
for f in glob.glob("*.csv"):
    csvs.append(f)

os.chdir(cwd)

sn=os.environ["SERVERNUM"]
wn=os.environ["WORKERNUM"]
    
try:
    subprocess.call(['hadoop','fs','-mkdir', 'hdfs://127.0.0.1:9000/worker'+sn+'_'+wn])
except Exception as e:
    print(e)

count = 0

for f in csvs:
    subprocess.call(['bash','runGI.bash','/home/mydata/'+f,'10'])
    os.chdir('tmp/')

    subprocess.call(['hadoop','fs','-mkdir','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+count])

    for x in glob.glob("*.JPG"):
        print(x)
    os.chdir(cwd)
    count += 1
