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

from hyperopt import hp, fmin, tpe, STATUS_OK, Trials

def updateOpt():
    hdfs = os.environ['HDFS']
    print('################################## Update ####################################')
    shutil.rmtree('/home/sim/Models')
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-get','hdfs://'+hdfs+':9000/Models', '/home/sim/'])

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
csvPos = 0
sn=os.environ["SERVERNUM"]
wn=os.environ["WORKERNUM"]

os.chdir('/home/mydata/')
for f in glob.glob("*.csv"):
    csvs.append(f)

os.chdir(cwd)

hdfs=os.environ['HDFS']

try:
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir', 'hdfs://'+hdfs+':9000/worker'+sn+'_'+wn])
except Exception as e:
    print(e)

count = 0
scores = []

#print([lastUpdate, count])

start = time.time()

DSVal = 1500
shutil.copyfile('/home/mydata/knn'+str(DSVal),'/home/sim/knndatasetGI')

epsilon  = 0.00001
params={
    '0000': hp.uniform('0000', epsilon, 1),
    '0001': hp.uniform('0001', epsilon, 1),
    '0010': hp.uniform('0010', epsilon, 1),
    '0011': hp.uniform('0011', epsilon, 1),
    '0100': hp.uniform('0100', epsilon, 1),
    '0101': hp.uniform('0101', epsilon, 1),
    '0110': hp.uniform('0110', epsilon, 1),
    '0111': hp.uniform('0111', epsilon, 1),
    '1000': hp.uniform('1000', epsilon, 1),
    '1001': hp.uniform('1001', epsilon, 1),
    '1010': hp.uniform('1010', epsilon, 1),
    '1011': hp.uniform('1011', epsilon, 1),
    '1100': hp.uniform('1100', epsilon, 1),
    '1101': hp.uniform('1101', epsilon, 1),
    '1110': hp.uniform('1110', epsilon, 1),
    '1111': hp.uniform('1111', epsilon, 1),
}

#DSVal = rangeCounter*500
shutil.copyfile('/home/mydata/knn'+str(DSVal),'/home/sim/knndatasetGI')

for i in range(16):
    bin = '{:04b}'.format(i)
    shutil.copyfile('/home/sim/knndatasetGI','/home/sim/Models/'+bin)

if(sn == '0' and wn == '0'):
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/Models','hdfs://'+hdfs+':9000/'])

def objective_function(params):
    global count, lastUpdateServ, lastUpdateGlob, csvPos, csvs, scores
    hdfs = os.environ['HDFS']
    score = 0
    for csv in csvs:
        pValues = ""
        for i in params:
            #pValues.append(params[i])
            pValues += str(params[i])+','

        f = csvs[csvPos]
        csvPos = (csvPos + 1) % len(csvs)

        print(pValues[:-1])

        if(count != 0):
            print("Checking for Updates")
            updateOpt()

        subprocess.call(['bash','runOpt.bash','/home/mydata/'+f, '80', str(100),'5', str(pValues[:-1])])
        os.chdir('tmp/')

        #subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir','hdfs://127.0.0.1:9000/worker'+sn+'_'+wn+'/run_'+str(count)])

        nImgs = len(glob.glob("*.csv"))

        f = open('/home/mydata/run_'+str(count), 'w+')
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

        weights = open('/home/sim/tmp/weights','w+')
        weights.write(pValues[:-1]+'\n')
        weights.close()

        #for x in glob.glob("*.*"):
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/tmp/','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/'])
        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mv','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/tmp','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/run_'+str(count)])

        subprocess.call(['/opt/hadoop/bin/hadoop','fs','-touchz','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/run_'+str(count)+"/done"])
        os.chdir(cwd)


        end = time.time()
        print("Time so far: "+str(end-start))
        count += 1

        energy = float(contents.split()[1])
        print(energy)
        score += energy

    scores.append(score)
    return {'loss': score,'status': STATUS_OK}

trials = Trials()
num_eval = 20
best_param = fmin(objective_function,
                    params,
                    algo=tpe.suggest,
                    max_evals=num_eval,
                    trials=trials,
                    rstate = np.random.RandomState(1))

end = time.time()
print(end-start)

f = open('/home/sim/tmp/runtime','w+')
f.write('Total Time: '+str(end-start)+'\n')
f.close()

subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','/home/sim/tmp/runtime','hdfs://'+hdfs+':9000/worker'+sn+'_'+wn+'/'])

print(best_param)
print(scores)
print('Done')
