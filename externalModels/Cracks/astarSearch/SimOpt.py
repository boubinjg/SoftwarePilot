import sys
import csv
from sklearn.neighbors import NearestNeighbors
import numpy as np
import statistics
from collections import defaultdict
import copy
import os
from shutil import copyfile
from shutil import rmtree
import glob
import subprocess
import random
import math
import traceback
import time
from operator import itemgetter

imagedata = []
knndataSet = []
profile = []
edgeProfile = []
netProfile = []
profileName = 'Spark.profile'
edgeProfileName = 'Profiles/4ci7.profile'
netProfileName = 'Profiles/5g.profile'

class Node:
        f = 0
        g = 0
        h = 0
        index = 0
        successor = 0
        def __init__(self,f,g,h,i,p,gi):
                self.f = f
                self.g = g
                self.h = h
                self.index = i
                self.parent = p
                self.gi = gi

def findLowestF(open):
        lowest = sys.maxsize
        lowestF = -1
        for node in open:
                if(node.f < lowest):
                        lowest = node.f
                        lowestF = node

        return lowestF


def gpsDist(coord1, coord2):
    R = 6372800  # Earth radius in meters
    lat1, lon1 = coord1
    lat2, lon2 = coord2

    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi       = math.radians(lat2 - lat1)
    dlambda    = math.radians(lon2 - lon1)

    a = math.sin(dphi/2)**2 + \
        math.cos(phi1)*math.cos(phi2)*math.sin(dlambda/2)**2

    return 2*R*math.atan2(math.sqrt(a), math.sqrt(1 - a))

def readInData(n):
    global knndataSet

    for i in range(n):
        f = "{0:04b}".format(i)
        try:
            print('Reading DS')
            ds = np.genfromtxt(sys.argv[2]+'/'+f,delimiter=',')
        except:
            print('DS Unreadable, Initializing Blank')
            ds = []

        knndataSet.append(ds)

    with open(profileName, 'rt', encoding='utf-8') as prof:
        reader = csv.reader(prof)
        profile.append(list(reader))

    with open(edgeProfileName, 'rt', encoding='utf-8') as edgeProf:
        reader = csv.reader(edgeProf)
        edgeProfile.append(list(reader))

    with open(netProfileName, 'rt', encoding='utf-8') as netProf:
        reader = csv.reader(netProf)
        netProfile.append(list(reader))

def findSamples(points):
    global imagedata
    samples = []
    for p in points:
        samples.append(imagedata[p])
    return samples

def getFeatures(image):
    feat = image[12][1:-1].split(',')
    knnfeat = ""
    for f in feat:
        knnfeat += str(f.split('=')[1]) + ','
    return np.fromstring(knnfeat[:-1], sep=',')

def findGIList(n, knndata):
    return knndata[n][13:17]

def findFeatures(n):
    return knndata[n][0:12]

def findDirection(ug, image):
    direct = -1
    nextPic = []
    for i in range(0,4):
        if ug[i] > direct:
            direct = ug[i]
            nextPic = image[i+4]
    return nextPic

def getErr(gi, fieldmap):
    mean = statistics.mean(fieldmap)
    median = statistics.median(fieldmap)
    rangeMean = (min(fieldmap)+max(fieldmap))/2
    if(len(fieldmap) > 1):
        CImin = mean - statistics.stdev(fieldmap)*1.96
        CImax = mean + statistics.stdev(fieldmap)*1.96
    else:
        CImin = float(gi)
        CImax = float(gi)

    return [mean, median, rangeMean, CImin, CImax]

def errChange(gi, inmap, oldErr):
    fieldmap = inmap[:]
    fieldmap.append(float(gi))

    stats = getErr(gi, fieldmap)
    newErr = max(stats) - min(stats)
    return oldErr - newErr #maximize this value

def findNext(image, knndata):
    query = getFeatures(image)

    nbors = NearestNeighbors(9)
    nbors.fit(knndata[:,0:12])

    print('^^^^^^^^^^^^^^^^^^^^^^^^^^^^^')
    print(len(knndata))
    print('^^^^^^^^^^^^^^^^^^^^^^^^^^^^^')

    knn = nbors.kneighbors([query[0:12]])
    knn = knn[1][0]
    dirs = [[0,0],[0,0],[0,0],[0,0]]
    for n in knn:
        ug = findGIList(n, knndata)
        for d in range(0,4):
            dirs[d][0] += 1
            dirs[d][1] += ug[d]

    mapGain = [d[1] / (d[0]-.00001) for d in dirs]

    gainMap = []
    for i in range (0,4):
        gainMap.append([mapGain[i], i])

    gainMap.sort(key=lambda gain: gain[0], reverse=False)
    print(gainMap)
    return gainMap

def findNextAstarWrapper(image, giTarg, profile, thresh):
    energies = []
    for neighbor in image[4:8]:
        if(neighbor != '[]'):
            img = getImage(neighbor)
            energy = findNextAstar(image, thresh, profile,thresh)
            #print(energy)
            #print(energy[0]/energy[1])
            energies.append(energy[0]/energy[1])
        else:
            energies.append('0')
    gainMap = []
    for i in range(0,len(energies)):
        if image[i+4] == '[]':
            gainMap.append([sys.maxsize,-1])
        else:
            gainMap.append([energies[i], image[i+4]])
    #print(gainMap)
    gainMap.sort(key=lambda gain: gain[0], reverse=False)
    print(gainMap)

    return gainMap

def findNextAstar(image, giTarg, profile,thresh):
    open = []
    closed = []

    query = getFeatures(image)

    start = Node(0,0,giTarg,query,-1,0)
    open.append(start)

    while(open != []):
        q = findLowestF(open)
        open.remove(q)

        nbors = NearestNeighbors(n_neighbors=5)
        nbors.fit(knndata[:,0:12])
        knn = nbors.kneighbors([q.index[0:12]])
        knn = knn[1][0]
        dirs = [[0,0],[0,0],[0,0],[0,0]]
        for n in knn:
            ug = findGIList(n)
            for d in range(0,4):
                dirs[d][0] += 1
                dirs[d][1] += ug[d]

        successorGain = [d[1] / (d[0]-.00001) for d in dirs]

        print(successorGain)
        #exit()

        for i in range(4):
            index = random.randint(0,len(knn)-1)

            sI = findFeatures(knn[random.randint(0,len(knn)-1)])
            gi = float(q.index[10])
            sG = q.g + (gi/successorGain[i])

            sH = ((q.g - gi)/gi) * (sum(profile)/len(profile))
            giAcc = q.gi+gi
            sF = sG + sH
            sP = q.index

            print(sG)

            #print(giAcc)

            if(giAcc > giTarg):
                return [sG, giAcc] #lowest E for direction, don't return

            else:
                skip = False
                for c in closed:
                    if((c.index[0:12] == q.index[0:12]).all() and c.f <= q.f):
                        skip = True
                if not skip:
                    s = Node(sF, sG, sH, sI, sP, giAcc)
                    open.append(s)

        #print([len(open),len(closed)])
        closed.append(q)
    highest = 0
    ret = None
    for i in closed:
        if(i.gi > highest):
            highest = i.gi
            ret = [i.g, i.gi]
    return ret

def findNextRand(image):

    dirs = [[0,0],[0,0],[0,0],[0,0]]

    gainMap = []
    for i in range (0,4):
        gainMap.append([random.uniform(0,1), i])

    gainMap.sort(key=lambda gain: gain[0], reverse=True)
    #print("gainmap")
    #print(gainMap)

    return gainMap


def findUtil(currentIm):
    #print(currentIm)
    return float(currentIm[12].split(',')[11].split('=')[1][:-1])

def getImage(imID):
    for d in imagedata:
        if d[1] == imID:
            return d
    return []

def getMap(sampleImages):
    count = 0
    giMap = defaultdict(list)
    #print(len(sampleImages))
    for image in sampleImages:
        try:
            gi = image[12].split(',')[10].split('=')[1]
            row = image[1].split(',')[0][1:]
            #print(row)
            giMap[int(row)].append(float(gi))
            count = count + 1
        except Exception as e:
            #print(e)
            pass

    return giMap

def setLoc(giMap, key, i):
    try:
        if(giMap[key][i] != 0):
            return giMap
    except: pass
    values = []
    try: values.append(giMap[key+1][i])
    except: pass
    try: values.append(giMap[key-1][i])
    except: pass
    try: values.append(giMap[key][i+1])
    except: pass
    try: values.append(giMap[key][i-1])
    except: pass
    try: values.append(giMap[key+1][i+1])
    except: pass
    try: values.append(giMap[key-1][i+1])
    except: pass
    try: values.append(giMap[key+1][i-1])
    except: pass
    try: values.append(giMap[key-1][i-1])
    except: pass

    values = list(filter((0).__ne__, values))
    giMap[key][i] = statistics.mean(values)

    return giMap

def dilateEightConnected(giMap, key, i):
    try: giMap = setLoc(giMap, key+1, i)
    except: pass
    try: giMap = setLoc(giMap, key-1, i)
    except: pass
    try: giMap = setLoc(giMap, key, i+1)
    except: pass
    try: giMap = setLoc(giMap, key, i-1)
    except: pass

    return giMap

def dilate(giMap):
    nMap = copy.deepcopy(giMap)
    for key in list(giMap.keys()):
        for i in range(0, len(giMap[key])):
            if(giMap[key][i] != 0):
                nMap = dilateEightConnected(nMap, key, i)
    return copy.deepcopy(nMap)

def isFull(giMap):
    for key in list(giMap.keys()):
        for i in range(0, len(giMap[key])):
            if giMap[key][i] == 0:
                return False
    return True

def fill(giMap):
    giMap = dilate(giMap)

    if(isFull(giMap)):
        return giMap
    else:
        giMap = fill(giMap)
    return giMap

def findSSD(giMap, curMap):
    val = 0
    zeros = 0
    for key in list(giMap.keys()):
        for i in range(0, len(giMap[key])):
            cv = (giMap[key][i] - curMap[key][i])**2
            if(cv == 0):
                zeros = zeros + 1
            val += cv
    #print("######################Zeros: "+str(zeros))
    return val

def parseCoord(coord):
    first = coord.split(',')[0][1:]
    second = coord.split(',')[1][0:-1]
    return [int(first), int(second)]

def manhattanDist(start, end):
    start = parseCoord(start)
    end = parseCoord(end)
    #print(start)
    #print(end)

    vert = abs(start[0]-end[0])
    horiz = abs(start[1]-end[1])

    return vert+horiz

def findLength(visited):
    totalDist = 0
    for i in range(0, len(visited)-1):
        totalDist += manhattanDist(visited[i], visited[i+1])
    return totalDist

def findClosest(image):
    closestIm = []
    closestDist = 100000
    lastIm = image
    for im in imagedata:
        if(im[1] not in visited):
            coord1 = im[2][1:-1].split(',')
            coord1 = [float(coord1[0]), float(coord1[1])]
            coord2 = lastIm[2][1:-1].split(',')
            coord2 = [float(coord2[0]), float(coord1[1])]
            distG = gpsDist(coord1, coord2)
            if(distG < closestDist):
                closestDist = distG
                closestIm = im
                currentIm = closestIm
    return closestIm

def findEnergy(image, visited, profile, gi):
    ret = []
    count = 0
    gi = float(image[12].split(',')[10].split('=')[1])
    try:
        for i in image[4:8]:
            if(i in visited):
                img = getImage(i)
                nGi = float(img[12].split(',')[10].split('=')[1])
                ret.append(nGi/gi)
            else:
                ret.append(1)
            count += 1
        return ret
    except:
        traceback.print_exc()


def writeUtilAst(visited, imdata, profile):
    try:
        rmtree('tmp')
    except Exception as e:
        print(e)

    os.mkdir("tmp")

    for im in visited:
        line = getImage(im)
        imageName = line[3][17:].split('/')[-1]
        gi = float(line[12].split(',')[10].split('=')[1][:-1])

        testVisited = copy.deepcopy(visited)
        testVisited.append(im)
        ret = []

        features = getFeatures(line)

        ret = features.tolist()

        for neighbor in line[4:8]:
            if(neighbor == '[]'):
                curImg = findClosest(image)
            else:
                curImg = getImage(neighbor)
            energy = findEnergy(curImg, testVisited, profile, gi)
            ret.append(min(energy))
        print('Ret')
        print(ret)
        fname = imageName.split('.')[0]
        with open('tmp/'+fname+'.csv','w') as f:
            writer = csv.writer(f)
            writer.writerow(ret)


def writeUtil(visited, imdata):
    #print("WRITE UTIL")
    #print(visited)
    for im in visited:
        for line in imdata:
            if(im == line[1]):
                imageName = line[3][17:].split("/")[-1]
                util = float(line[12].split(',')[12].split('=')[1][:-1])
                #print(util)
                utilList = []
                for i in range(4):
                    im2 = line[i+4]
                    if(im2 in visited):
                        im2Line = getImage(im2)
                        im2util = float(im2Line[12].split(',')[12].split('=')[1][:-1])
                        utilList.append(im2util/util)
                    else:
                        utilList.append('1')
                #print(imageName)
                #print(utilList)
                fname = imageName.split('.')[0]
                #print(fname)
                with open('tmp/'+fname+'.csv','w') as f:
                    writer = csv.writer(f)
                    writer.writerow(utilList)

def writeEnergy(energy,edgeEnergy, ft, tput):
    print('Writing Energy')
    f = open('tmp/energy','w+')
    f.write('Energy: '+str(energy)+'\n')
    f.write('EdgeEnergy: '+str(edgeEnergy)+'\n')
    f.write('FlightTime: '+str(ft)+'\n')
    f.write('Throughput: '+str(tput)+'\n')
    f.close()

def writeIms(visited,imdata):
    #print(visited)
    print('Writing Ims')
    try:
        rmtree('tmp')
    except Exception as e:
        print(e)

    os.mkdir("tmp")

    for im in visited:
        for line in imdata:
            if(im == line[1]):
                imagePath = line[3][20:]
                imagePath = "/home/imageData/"+imagePath
                fileName = imagePath.split("/")[-1]
                #print(line[3])
                #print(imagePath)
                #print(fileName)
                copyfile(imagePath,"tmp/"+fileName)


#def writeHDFS():
#    exe = sys.argv[4]
#    workerNum = os.environ["WORKERNUM"]
#    serverNum = os.environ["SERVERNUM"]
#    direc = str(serverNum)+"_"+str(workerNum)+"_"+str(exe)+"/"
#    subprocess.call(["hadoop","fs","-mkdir","hdfs://127.0.0.1:9000/"+direc])
#    for f in glob.glob("/home/sim/tmp/*"):
#        subprocess.call(["hadoop","fs","-put",f,"hdfs://127.0.0.1:9000/"+direc])

def get_size(start_path):
    total_size = 0
    for dirpath, dirnames, filenames in os.walk(start_path):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            # skip if it is symbolic link
            if not os.path.islink(fp):
                total_size += os.path.getsize(fp)
    return total_size

def actionSleep(sleepTime):
    time.sleep(sleepTime)
    return 0

if __name__ == '__main__':
    numUAV = sys.argv[1]
    numModels = int(numUAV)**2
    knndatasetLoc = sys.argv[2]

    imageFile = open('inputImage')
    image = imageFile.read()
    imageFile.close()

    weightsFile = open('inputWeights')
    weights = [float(item) for item in weightsFile.read().split(',')]
    weightsFile.close()

    readInData(numModels)

    imageProc = [float(item.split('=')[1]) for item in image.split(',')]

    print(imageProc)

    print(numUAV, image)

    print('Exploring')
    maps = []
    for i in range(numModels):
        if(knndataSet[i] == []):
            print('Random Explore')
            gm = findNextRand(imageProc)
            maps.append(gm)
        else:
            print('Inference')
            gm = findNext(imageProc, knndataSet[i])
            maps.append(gm)
    gainMap = []
    for i in maps[0]:
        gainMap.append([0,i[1]])
        for i in range(len(maps)):
            for j in range(len(gainMap)):
                slot = maps[i][j][1]
                for k in range(len(gainMap)):
                        if(gainMap[k][1] == slot):
                            gainMap[k][0] += maps[i][j][0] * weights[i]
                            #print(slot, gainMap[k][1])
                            #print('$$$$$$$$$$$$$$$$$$$$')


    gainMap.sort(key=lambda gain: gain[0], reverse=False)
    print(gainMap)
