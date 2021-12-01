import argparse
from fcwaypoint import WaypointSet
from spwrapper import Aircraft
import os
from shutil import copyfile
import subprocess
import time
import numpy as np
######################__<HYPERPARAMETERS>__#################################
#
#Pre-defined state-group value for this experiment set
NUM_STATE_GROUPS=9
#Utility Maximum per SG
Ui_MAX = 1000
#Visited States Maximum per SG
Vi_MAX = 1
######################__</HYPERPARAMETERS>__#################################
weights = []

def writeMissionToHDFS():
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-mkdir', 'hdfs://master:9820/worker1/'])
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-put','tmp/export', 'hdfs://master:9820/worker1/'])
    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-touchz', 'hdfs://master:9820/worker1/done'])

def renameImg(wpNum, SGNum):
    cwd = os.getcwd()
    print(wpNum, SGNum)
    copyfile(cwd+'/tmp/img.JPG', cwd+'/tmp/export/IMG_'+str(SGNum)+'_'+str(wpNum)+'.JPG')

def exploreSG(SG, drone, SGNum):
    currentWaypoint, wpNum = SG.getStart()
    Ui = 0
    Vi = 0
    while(Ui < Ui_MAX and Vi < Vi_MAX):
        SG.markVisited(wpNum)
        #TODO: Test this line vvv
        #drone.flyToWaypoint(currentWaypoint.lat, currentWaypoint.lon, 5)

        #This works vvv
        #image = sp.captureImage()

        fv = drone.extractFeatures()
        utility = fv[-1]
        #print(fv, utility)
        currentWaypoint.setValue(utility)
        #print(currentWaypoint.value)

        Ui += utility

        qValues = drone.getQVals(fv)
        #print(qValues)

        Vi += 1
        if(Vi < Vi_MAX):
            currentWaypoint, wpNum = SG.getNextWaypoint(qValues, wpNum)

        renameImg(wpNum, SGNum)

def writeMetrics(runtime, wpset, fieldMap, batt):
    visited = 0
    for i in range(len(wpset.sgList)):
        visited += len(wpset.getSG(i).visited)

    f = open('tmp/metrics.txt', 'w')
    f.write('Runtime: '+str(runtime)+'\n')
    f.write('Waypoints Visited: '+str(visited)+'\n')
    f.write('Battery: '+str(batt)+'\n')
    f.close()

    with open('tmp/map.npy', 'wb') as f:
        np.save(f, fieldMap)

    return 0

def runMission(wpset, aircraft):
    for currentSGNum in range(NUM_STATE_GROUPS):
        print('Visiting State Group '+str(currentSGNum))
        currentSG = wpset.getSG(currentSGNum)
        exploreSG(currentSG, aircraft, currentSGNum)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('waypointfile', metavar='w', type=str, help='Mission Waypoint File')
    parser.add_argument('ip', metavar='ip',type=str, help='IP address of SP Installation')

    args = parser.parse_args()

    #Initialize waypoint set (waypoints and state-groups) using provided waypoint file
    wpset = WaypointSet(args.waypointfile)

    drone = Aircraft(args.ip)
    #drone = None

    starttime = time.time()
    runMission(wpset, drone)
    runtime = time.time() - starttime
    #Add Battery Stuff HERE
    batt = 0 #getBatt()


    fieldMap = wpset.buildMap()
    print(fieldMap)

    wpset.saveNeighborUtilities(fieldMap)

    writeMetrics(runtime, wpset, fieldMap, batt)

    writeMissionToHDFS()

    print('mission')
