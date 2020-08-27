import subprocess
import os
import argparse
import time
from operator import add

DATA_LOC='/home/boubin/Images/'
CUBE_LOC='/home/boubin/SoftwarePilot/DistributedRL/Data/'

def consoleLog(string):
	print("#################################################")
	print("##############DRL Controller:")
	print(string)
	print("#################################################")

#Start HDFS
def startHDFS():
	consoleLog("DRL Controller Loading HDFS Containers")
	pwd = os.getcwd()
	os.chdir('../docker-hadoop')
	subprocess.call(['docker-compose','up', '-d'])
	os.chdir(pwd)
	consoleLog("HDFS Loaded")

#Start Servers
def startServers(numServs):
	serverList = []
	pwd = os.getcwd()
	os.chdir('../Gateway')
	for i in range(0, numServs):
		#subprocess.call(['docker', 'run', '--net=host', '-e SERVERNUM='+str(i),
		#		 '--name','server'+str(i), 'spcn', '/bin/bash'])
		subprocess.call(['bash','runGateway_Sim.sh', str(i),'server'+str(i)])
		consoleLog("Server" + str(i)+" Started")
		serverList.append("server"+str(i))
	os.chdir(pwd)
	return serverList

def stopAggregators(numAgs):
    aggCmd = 'kubectl delete pods '
    for i in range(1, numAgs+1):
        models = '{:04b}'.format(i)
        file = 'aggregator'+str(models)+' '
        aggCmd += file
    out = os.popen(aggCmd).read()
    print(out)

def startAggregators(numAgs, priorities):
    aggList = []

    for i in range(1,numAgs+1):
        priority = priorities[i-1]
        models = '{:04b}'.format(i)
        baseConfig = 'aggregator'+str(models)+'.yaml'
        f = open(baseConfig,'r')
        config=f.read()
        config+='    priorityClassName: drl'+str(priority)
        f.close()
        f = open('tmp.yaml','w+')
        f.write(config)
        f.close()

        subprocess.call(['kubectl', 'apply', '-f', 'tmp.yaml'])
        consoleLog("Aggregator "+str(models)+" Started with priority "+str(priority))
    return aggList
#Start workers and servers
def startWorkers(numServs, numWorkers):
    workerList = []
    pwd = os.getcwd()
    os.chdir('../Worker')
    for i in range(0, numServs):
        for j in range(0,numWorkers):
            subprocess.call(['bash','runWorker_Sim.sh',str(i), str(j), "worker"+str(i)+'_'+str(j), '184.57.4.230'])
            #subprocess.call(['kubectl','apply','-f',"worker"+str(i)+''+str(j)+'.yaml'])
            consoleLog("Worker" + str(i)+'_'+str(j)+" Started")
            workerList.append("worker"+str(i)+'_'+str(j))
    os.chdir(pwd)
    return workerList

def startGlobal():
	pwd = os.getcwd()
	os.chdir("../Global")
	subprocess.call(['bash','runGlobal_Sim.sh',"global"])
	consoleLog("Global Started")

def checkForWeights(ip, count):
    found = 0
    jobs = [[0,0],[0,1],[1,0],[1,1]]
    while(found < len(jobs)):
        found = 0
        foundNums = []
        for i in jobs:
            out = os.popen('hadoop fs -test -e hdfs://'+ip+':9000/worker'+
                            str(i[0])+'_'+str(i[1])+'/run_'+str(count)+'/done && echo $?').read()
            try:
                if(int(out) == 0 and i not in foundNums):
                    found += 1
                    foundNums.append(i)
                    print("server "+str(i[0]) + " Worker "+str(i[1])+" Job "+str(count) + " Complete")
            except:
                    print("server "+str(i[0]) + " Worker "+str(i[1])+" Job "+str(count) + " incomplete")
        time.sleep(10)

def downloadWeights(ip, count):
    jobs = [[0,0],[0,1],[1,0],[1,1]]
    try:
        rmtree('tmp')
    except:
        pass
    try:
        os.mkdir('tmp')
    except:
        pass
    for i in jobs:
        cmd = 'hadoop fs -get hdfs://'+ip+':9000/worker'+str(i[0])+'_'+str(i[1])+'/run_'+str(count)+' tmp/'+str(i[0])+str(i[1])
        out = os.popen(cmd).read()
        print(out)

def getPriorities():
    weightList = []
    dirs = ['00','01','10','11']
    for i in dirs:
        f = open('tmp/'+i+'/weights')
        weights = [float(item) for item in f.read().split(',')]
        if(weightList == []):
            weightList = weights
        else:
            weightList = list(map(add, weightList, weights))

    retList = [int(10*(a/len(dirs))) for a in weightList]
    return retList
#start simulation

#Stop Servers
def stopServers(serverList):
	for server in serverList:
		subprocess.call(["docker", "rm", "-f", server])
		consoleLog("Server " + server + " Stopped")

#Stop Workers
def stopWorkers(workerList):
	for worker in workerList:
		subprocess.call(["docker", "rm", "-f", worker])
		consoleLog("Worker " + worker + " Stopped")

def stopGlobal():
	subprocess.call(["docker","rm","-f","global"])
	consoleLog("Global Stopped")

if __name__ == "__main__":
    consoleLog("DRL Controller Starting")

    consoleLog("Starting Global")
    #startGlobal()
    consoleLog("Starting Workers")
    workerList = startWorkers(2, 2)

    for i in range(0,19):
        checkForWeights('184.57.4.230', i*5)
        downloadWeights('184.57.4.230', i*5)
        priorities = getPriorities()
        aggPriorities = priorities[1:]
        stopAggregators(15)
        startAggregators(15, aggPriorities)
