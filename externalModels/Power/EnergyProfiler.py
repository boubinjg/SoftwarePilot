import sys
import numpy as np
DroneEnergy = 0
ArchEnergy = 0
droneActions = {}
archActions = {}
DroneBatteryVoltage = 0
DroneBatteryMAH = 0
ArchBatteryMAH = 0
ArchBatteryVoltage = 0
totalTime = 0
archTime = 0
droneTime = 0

hoverEnergy = 0
flyEnergy = 0
edgeIdleEnergy = 0
edgeCPUEnergy = 0
networkEnergy = 0

dtc = 0

def normal(time, sd):
    nTime= np.random.normal(time,sd,1)
    if abs(nTime - time) < sd*2.5 and nTime > 0:
        return nTime[0]
    return normal(time, sd)

def readDrone(fname):
    global DroneBatteryVoltage
    global DroneBatteryMAH
    with open(fname) as f:
        content = f.readlines();
    content = [x.strip() for x in content]
    for line in content:
        parsedLine = line.split('=')
        if parsedLine[0].strip() == "BatteryMAH":
            DroneBatteryMAH = (float)(parsedLine[1])
        elif parsedLine[0].strip() == "BatteryVoltage":
            DroneBatteryVoltage = (float)(parsedLine[1])
        else:
            action = parsedLine[0].strip()
            energy = (float)(parsedLine[1].split(",")[0].strip())
            time = (float)(parsedLine[1].split(",")[1].strip())
            droneActions[action] = (energy, time)

def readArch(fname):
    global ArchBatteryMAH
    global ArchBatteryVoltage
    with open(fname) as f:
        content = f.readlines();
    content = [x.strip() for x in content]
    for line in content:
        parsedLine = line.split('=')
        if parsedLine[0] == "BatteryMAH":
            ArchBatteryMAH = (float)(parsedLine[1])
        elif parsedLine[0] == "BatteryVoltage":
            ArchBatteryVoltage = (float)(parsedLine[1])
        else:
            action = parsedLine[0].strip()
            energy = (float)(parsedLine[1].split(",")[0].strip())
            time = (float)(parsedLine[1].split(",")[1].strip())
            sd = (float)(parsedLine[1].split(",")[2].strip())
            archActions[action] = (energy, time, sd)

def readMissionSeq(fname):
    global DroneEnergy, ArchEnergy, totalTime, droneTime, archTime, dtc
    global hoverEnergy, flyEnergy, networkEnergy, edgeCPUEnergy, edgeIdleEnergy
    with open(fname) as f:
        content = f.readlines();
    content = [x.strip() for x in content]
    for line in content:
        totalTime += .37
        hoverEnergy += (droneActions["Hover"][0] * .33)
        ArchEnergy += (archActions["Idle"][0] * .33)
        networkEnergy += (archActions["Idle"][0] * .33)
        if line in archActions:
            time = normal(archActions[line][1], archActions[line][2])
            DroneEnergy += (droneActions["Hover"][0]) * time
            hoverEnergy += (droneActions["Hover"][0]) * time
            ArchEnergy += (archActions[line][0]) * time
            if line == "SendImage":
                networkEnergy += (archActions[line][0]) * time
            else:
                edgeCPUEnergy += (archActions[line][0]) * time
            archTime += time
            totalTime += time
        elif line in droneActions:
            DroneEnergy += (droneActions[line][0]) * (droneActions[line][1])
            ArchEnergy += (archActions["Idle"][0]) * (droneActions[line][1])
            droneTime += droneActions[line][1]
            dtc = dtc+1
            totalTime += droneActions[line][1]
            flyEnergy += droneActions[line][0] * (droneActions[line][1])
            edgeIdleEnergy += (archActions["Idle"][0]) * (droneActions[line][1])
            #print(edgeIdleEnergy)
        else:
            print("Illegal Action" + line)


readDrone(sys.argv[1])
readArch(sys.argv[2])
readMissionSeq(sys.argv[3])
DroneMAH = DroneEnergy/3600 * 1000/DroneBatteryVoltage;
DroneBU = DroneMAH/DroneBatteryMAH


ArchMAH = ArchEnergy/3600 * 1000/ArchBatteryVoltage;
ArchBU = ArchMAH/ArchBatteryMAH

print("Time="+str(totalTime))
print("ComputeLatency "+str(droneTime))
print("ArchLatency "+str(archTime))
print("Drone BU="+str(DroneBU))
print("DroneMAH="+str(DroneMAH))
print("Architecture BU="+str(ArchBU))
print("Architecture Energy="+str(ArchEnergy))
print("Drone Energy="+str(DroneEnergy))

totalEnergy = ArchEnergy + DroneEnergy
print("Hover Energy="+str(hoverEnergy/totalEnergy))
print("Fly Energy="+str(flyEnergy/totalEnergy))
print("Network Energy="+str(networkEnergy/totalEnergy))
print("Edge CPU Energy="+str(edgeCPUEnergy/totalEnergy))
print("Edge Idle Energy="+str(edgeIdleEnergy/totalEnergy))
print(totalEnergy)
print(dtc)
print(10/DroneBU)
