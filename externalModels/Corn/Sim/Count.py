import sys
import csv
from sklearn.neighbors import NearestNeighbors
import numpy as np
import statistics
import math

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

if __name__ == '__main__':
    readInData()

    #find test all sample points
    sampleVec = [i for i in range(int(len(imagedata)/4*3), len(imagedata))]

    sampleImages = findSamples(sampleVec);

    count = 0
    giList = []
    print(len(sampleImages))
    for image in sampleImages:
        try:
            print(image[12])
            gi = image[12].split(',')[10].split('=')[1]
            giList.append(float(gi))
            count = count + 1
        except Exception as e:
            print(e)
            pass

    count = len(giList)
    mean = statistics.mean(giList)
    std = statistics.stdev(giList)
    print([mean, std, min(giList), max(giList), count])
