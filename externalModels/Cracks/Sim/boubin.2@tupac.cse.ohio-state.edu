import sys
import csv
from sklearn.neighbors import NearestNeighbors
import numpy as np
import statistics
from collections import defaultdict
import copy

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

if __name__ == '__main__':
    readInData()
    imagedata = imagedata[1:]
    #find test all sample points
    #offset = #Jun
    #offset = #Aug
    #offset = 22 #Sep, Jun
    offset = -88 #aug
    sampleVec = [i for i in range(int(len(imagedata)/4*3)-offset, len(imagedata))]
    #Sep
    #testSamples = ["[33,2]","[33,4]","[33,5]","[33,6]","[33,7]","[33,8]","[33,9]","[33,13]","[34,0]","[34,1]","[34,2]","[34,3]","[35,1]"]
    #Jun
    #testSamples = ["[45,36]","[45,37]","[45,38]","[46,4]","[44,39]","[46,15]","[43,45]","[44,36]","[41,38]","[41,43]"]
    #testSamples = ["[46,4]","[44,39]","[46,15]","[43,45]","[44,36]","[41,38]","[41,43]"]
    #Aug
    testSamples = ["[38,2]","[39,1]","[39,22]","[39,23]","[40,1]","[42,0]","[43,0]","[44,0]","[45,0]","[45,2]"]

    print(len(sampleVec))
    sampleImages = findSamples(sampleVec);
    sampleSubset = []
    for sample in sampleImages:
        if(sample[1] in testSamples):
            sampleSubset.append(sample)

    print(sampleImages[0][1])

    GIMap = getMap(sampleImages)
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
    for image in sampleSubset:
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
                #print([cErr,count])

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
                                print(str(e))
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

        for v in range(0, len(visited)):
            row = int(visited[v].split(',')[0][1:])
            col = int(visited[v].split(',')[1][0:-1])
            try:
                curMap[row][col] = fieldMap[v]
            except:
                pass #row,col is not in range of sample space
        #print(curMap)
        #print(GIMap)
        curMap = fill(curMap)
        #print(curMap)

        curSSD = findSSD(curMap, GIMap)
        SSD.append(curSSD)
        SSDList.append([curSSD,image[1]])
        if(count == runs):
            print("----------------FULL------------------")
            fullList.append([curSSD, image[1]])
        print(curSSD)

        length.append(findLength(visited))

mean = statistics.mean(means)
sd = statistics.stdev(means)

meanSSD =statistics.mean(SSD)
sdSSD = statistics.stdev(SSD)

SSDList.sort()
for item in SSDList:
    print(item)

print("-------------------------FULL LIST--------------------------------")
for item in fullList:
    print(item)

print("-------------------------LENGTH------------------------------------")

for item in length:
    print(item)

print(['mean', mean], ['stdev', sd], ['min',min(means)], ['max',max(means)])
print(['SSD mean', meanSSD], ['SSD stdev', sdSSD])
print("Waypoints: "+str(wpoints) + "Missions: "+str(missions))
