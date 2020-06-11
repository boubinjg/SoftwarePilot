import os, glob
import subprocess
from os import listdir
from os.path import isfile, join
import time


cwd = os.getcwd()
csvs = []
os.chdir("/home/mydata/")
for f in glob.glob("*.csv"):
    csvs.append(f)

os.chdir(cwd)

sn=os.environ["SERVERNUM"]
wn=os.environ["WORKERNUM"]

count = 0
energyList = []
for f in csvs:
    print('File: '+f)
    energyList.append([f])
    for rangeCounter in range(0,10):
        with open(os.devnull, 'w') as outf:
            subprocess.call(['bash','runGI.bash','/home/mydata/'+f, '60', str(100), str(count)], stdout=outf, stderr = outf)
        os.chdir('tmp/')

        energy = open('/home/sim/tmp/energy','r+')
        energyVal = float(energy.read().split(' ')[1])
        energy.close()

        os.remove('/home/sim/tmp/energy')

        energyList[count].append(energyVal)
        os.chdir(cwd)
        print(energyVal)

    count += 1
print(" ")

#print(energyList)
for i in energyList:
    for j in i:
        print(j)
    print('')
