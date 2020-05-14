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

imagedata = []
knndata = None

def readInData():
    global imagedata, knndata
    with open(sys.argv[1], 'rt', encoding="utf-8") as data:
        reader = csv.reader(data)
        imagedata.append(list(reader))

    imagedata = imagedata[0]

    knndata = np.genfromtxt(sys.argv[2], delimiter=',')

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

def findGIList(n):
    return knndata[n][12:16]

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

def findNext(image, fieldmap, oldErr):
    query = getFeatures(image)

    nbors = NearestNeighbors(9)
    nbors.fit(knndata[:,0:12])

    knn = nbors.kneighbors([query[0:12]])
    knn = knn[1][0]
    dirs = [[0,0],[0,0],[0,0],[0,0]]
    for n in knn:
        ug = findGIList(n)
        for d in range(0,4):
            dirs[d][0] += 1
            dirs[d][1] += errChange(ug[d], fieldmap, oldErr)

    mapGain = [d[1] / (d[0]-.00001) for d in dirs]

    gainMap = []
    for i in range (0,4):
        gainMap.append([mapGain[i], image[i+4]])

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
    print(len(sampleImages))
    for image in sampleImages:
        try:
            gi = image[12].split(',')[10].split('=')[1]
            row = image[1].split(',')[0][1:]
            print(row)
            giMap[int(row)].append(float(gi))
            count = count + 1
        except Exception as e:
            print(e)
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
    print("######################Zeros: "+str(zeros))
    return val

def parseCoord(coord):
    first = coord.split(',')[0][1:]
    second = coord.split(',')[1][0:-1]
    return [int(first), int(second)]

def manhattanDist(start, end):
    start = parseCoord(start)
    end = parseCoord(end)
    print(start)
    print(end)

    vert = abs(start[0]-end[0])
    horiz = abs(start[1]-end[1])

    return vert+horiz

def findLength(visited):
    totalDist = 0
    for i in range(0, len(visited)-1):
        totalDist += manhattanDist(visited[i], visited[i+1])
    return totalDist

def writeUtil(visited, imdata):
    print("WRITE UTIL")
    print(visited)
    for im in visited:
        for line in imdata:
            if(im == line[1]):
                imageName = line[3][17:].split("/")[-1]
                #util = float(line[12].split(',')[12].split('=')[1][:-1])
                #print(util)
                utilList = []
                for i in range(4):
                    im2 = line[i+4]
                    if(im2 in visited):
                        im2Line = getImage(im2)
                        im2util = float(im2Line[12].split(',')[12].split('=')[1][:-1])
                        utilList.append(im2util)
                    else:
                        utilList.append('0')
                print(imageName)
                print(utilList)
                fname = imageName.split('.')[0]
                print(fname)
                with open('tmp/'+fname+'.csv','w') as f:
                    writer = csv.writer(f)
                    writer.writerow(utilList)

def writeIms(visited,imdata):
    print(visited)
    try:
        rmtree('tmp')
    except Exception as e:
        print(e)
    
    os.mkdir("tmp")

    for im in visited:
        for line in imdata:
            if(im == line[1]):
                imagePath = line[3][17:]
                imagePath = "/home/imageData/"+imagePath
                fileName = imagePath.split("/")[-1]
                copyfile(imagePath,"tmp/"+fileName)


#def writeHDFS():
#    exe = sys.argv[4]
#    workerNum = os.environ["WORKERNUM"]
#    serverNum = os.environ["SERVERNUM"]
#    direc = str(serverNum)+"_"+str(workerNum)+"_"+str(exe)+"/"
#    subprocess.call(["hadoop","fs","-mkdir","hdfs://127.0.0.1:9000/"+direc])
#    for f in glob.glob("/home/sim/tmp/*"):
#        subprocess.call(["hadoop","fs","-put",f,"hdfs://127.0.0.1:9000/"+direc])


if __name__ == '__main__':
    readInData()
    imagedata = imagedata[1:]
   
    GIMap = getMap([imagedata[0]])
	
    blankMap = defaultdict(list)

    for key in list(GIMap.keys()):
        for i in range(0, len(GIMap[key])):
            blankMap[key].append(0)

    print(blankMap)

    runs = float(sys.argv[3])

    wpoints = 0
    missions = 0
    cErr = 1

    count = 0
    means = []
    SSD = []
    SSDList = []
    fullList = []
    length = []
    print("Start")
    #for image in sampleImages:
    for image in [imagedata[1]]:
        fieldMap = []
        visited = []
        stack = []
        curMap = copy.deepcopy(blankMap)

        missions += 1
        currentIm = image
        #currentIm = sampleImages[1]
        wpoints += count
        count = 0

        try:
            while(count < runs):
                try:
                    gi = currentIm[12].split(',')[10].split('=')[1]
                except:
                    gi = 0
                fieldMap.append(float(gi))
                visited.append(currentIm[1])

                stats = getErr(gi, fieldMap)
                cErr = max(stats)-min(stats)
                print([cErr,count])

                if(count < runs):
                    gainMap = findNext(currentIm, fieldMap, cErr) #redo this to factor in error-gain
                    fin = False
                    #print([cErr, count])

                    stack.append(currentIm)

                    while not fin and stack != []:
                        for i in range(0,4):
                            try:
                                testIm = getImage(gainMap[i][1])

                                if (len(testIm[12].split(','))) != 13:
                                    raise Exception('NaN Value in Dataset Handled')
                                if (testIm[1] in visited):
                                    raise Exception('previously visited')
                                currentIm = testIm

                                fin = True
                                break;

                            except Exception as e:
                                print("Exception: " + str(e))
                                pass

                        if not fin:
                            currentIm = stack.pop()
                    if not fin:
                        raise Exception("Invalid Move: No Movements Possible")
                else:
                    break
                count = count + 1

        except Exception as e:
            print(str(e))
            print("Hit the bottom")
            if(count > 0):
                print(str(count) + " " + str(missions))
            pass

        print("Missions: "+str(missions) + " waypoints: "+str(count) + " SampleGI: "+str(statistics.mean(fieldMap)))
        mean = statistics.mean(fieldMap)
        means.append(mean)

        writeIms(visited, imagedata)
        writeUtil(visited, imagedata)
        #writeHDFS()
        
        for v in range(0, len(visited)):
            row = int(visited[v].split(',')[0][1:])
            col = int(visited[v].split(',')[1][0:-1])
            try:
                curMap[row][col] = fieldMap[v]
            except:
                pass #row,col is not in range of sample space

print("Mean GI:" + str(mean))
