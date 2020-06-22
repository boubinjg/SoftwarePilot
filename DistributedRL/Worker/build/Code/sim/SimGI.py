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
imagedata = []
knndata = None
profile = []
profileName = 'Spark.profile'

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

def readInData():
    global imagedata, knndata
    with open(sys.argv[1], 'rt', encoding="utf-8") as data:
        reader = csv.reader(data)
        imagedata.append(list(reader))

    imagedata = imagedata[0]
    try:
        knndata = np.genfromtxt(sys.argv[2], delimiter=',')
    except:
        knndata = []

    with open(profileName, 'rt', encoding='utf-8') as prof:
        reader = csv.reader(prof)
        profile.append(list(reader))

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

def findNext(image, fieldmap, oldErr):
    query = getFeatures(image)

    nbors = NearestNeighbors(int(sys.argv[5]))
    nbors.fit(knndata[:,0:12])

    knn = nbors.kneighbors([query[0:12]])
    knn = knn[1][0]
    dirs = [[0,0],[0,0],[0,0],[0,0]]
    for n in knn:
        ug = findGIList(n)
        for d in range(0,4):
            dirs[d][0] += 1
            dirs[d][1] += ug[d]

    mapGain = [d[1] / (d[0]-.00001) for d in dirs]

    gainMap = []
    for i in range (0,4):
        if(image[i+4] == '[]'):
            gainMap.append([sys.maxsize,-1])    
        else:
            gainMap.append([mapGain[i], int(image[i+4])])
        
    gainMap.sort(key=lambda gain: gain[0], reverse=False)
    print(gainMap)
    return gainMap

def findNextAstarWrapper(image, giTarg, profile,thresh):
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

def findNextRand(image, fieldmap, oldErr):

    dirs = [[0,0],[0,0],[0,0],[0,0]]

    gainMap = []
    for i in range (0,4):
        gainMap.append([random.uniform(0,1), image[i+4]])

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

def writeEnergy(energy):
    print('Writing Energy')
    f = open('tmp/energy','w+')
    f.write('Energy: '+str(energy))
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


if __name__ == '__main__':
    readInData()
    imagedata = imagedata[:]
   
    GIMap = getMap([imagedata[0]])
	
    blankMap = defaultdict(list)

    for key in list(GIMap.keys()):
        for i in range(0, len(GIMap[key])):
            blankMap[key].append(0)

    #print(blankMap)

    runs = float(sys.argv[3])
    seed = float(sys.argv[4])
    random.seed(seed)

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
    #pos = random.randint(0, len(imagedata)-1)
    pos = 15
    giGain = 0
    energy = 0
    prof = []
    prof.append(profile[0][6])
    prof.append(profile[0][3])
    prof.append(profile[0][7])
    prof.append(profile[0][2])
    for i in range(4):
        entry = prof[i]
        j = float(entry[0].split('=')[1])
        t = float(entry[1])
        prof[i] = j * t
    

    for image in [imagedata[pos]]:
        fieldMap = []
        visited = []
        stack = []
        curMap = copy.deepcopy(blankMap)

        missions += 1
        currentIm = image
        #currentIm = sampleImages[1]
        wpoints += count
        count = 0
        giTarg = 45
        #print("giTarg "+str(giTarg))
        totalGi = 0
        gi = 100
        giOrig = 1

        try:
            while(count < runs and gi > giTarg):
                gi_old = gi
                try:
                    gi = float(currentIm[12].split(',')[10].split('=')[1])
                except:
                    gi = 0
                    
                if(gi_old != -1):
                    giGain += (gi-gi_old)
                else:
                    giOrig = gi

                totalGi += gi
                
                print("::::::::::::::::::::::::::::::::::::: GI: "+str(count)+":::::::::::::::::::::::::::::::::::::")
                
                print("::::::::::::::::::::::::::::::::::::: GI: "+str(gi)+":::::::::::::::::::::::::::::::::::::")
                #print("::::::::::::::::::::::::::::::::::::: Total GI: "+str(totalGi)+":::::::::::::::::::::::::::::::::::::")
 
                #print("::::::::::::::::::::::::::::::::::::: GI Target: "+str(giTarg)+":::::::::::::::::::::::::::::::::::::")
 
                #print("::::::::::::::::::::::::::::::::::::: Total Relative GI: "+str(totalGi/giOrig)+":::::::::::::::::::::::::::::::::::::")


                fieldMap.append(float(gi))
                visited.append(currentIm[1])

                stats = getErr(gi, fieldMap)
                cErr = max(stats)-min(stats)
                #print([cErr,count])

                if(count < runs):
                    lastIm = currentIm
                    if(knndata == []):
                        print("RANDOM EXPLORE")
                        gainMap = findNextRand(currentIm, fieldMap, cErr)
                    else:
                        gainMap = findNext(currentIm, fieldMap, cErr) #redo this to factor in error-gain
                        #print(gainMap)
                    fin = False
                    #print([cErr, count`])

                    stack.append([currentIm, gainMap])
                    while not fin and stack != []:
                        for i in range(0,4):
                            try:
                                testIm = getImage(str(gainMap[i][1]))
    
                                if (len(testIm[12].split(','))) != 13:
                                    raise Exception('NaN Value in Dataset Handled')
                                if (testIm[1] in visited):
                                    raise Exception('previously visited')
                                currentIm = testIm

                                fin = True
                                energy += prof[i]
                                print(prof[i])
                                print(energy)
                                break;
                                
                            except Exception as e:
                                print("Exception: " + str(e))
                                traceback.print_exc()
                                pass
 
                        #if not fin and stack != []:
                        #    print('###########################################################')
                        #    print(len(stack))
                        #    last = stack.pop()
                        #    currentIm = stack[0]
                        #    gainmap = stack[0]

                        if not fin:
                            closestIm = []
                            closestDist = 100000
                            print('%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%')
                            currentIm = lastIm
                            for im in imagedata:
                                if(im[1] not in visited):
                                    #print("FINDING NEXT: ")
                                    coord1 = im[2][1:-1].split(',')
                                    coord1 = [float(coord1[0]), float(coord1[1])]
                                    coord2 = lastIm[2][1:-1].split(',')
                                    coord2 = [float(coord2[0]), float(coord1[1])]
                                    distG = gpsDist(coord1, coord2)
                                    if(distG < closestDist):
                                        closestDist = distG
                                        closestIm = im
                                    #print('distG: '+str(distG))
                                    #print([coord1, coord2])
                                    #print('Dist: '+str(closestDist))
                            currentIm = closestIm
                            fin = True
                            if(closestDist < 10):
                                energy += sum(prof)/len(prof)
                                print(sum(prof)/len(prof)*closestDist/10.0)
                                print(closestDist)
                            else:
                                energy += (sum(prof)/len(prof)) * (closestDist/10.0)
                                print(sum(prof)/len(prof)*closestDist/10.0)
                                print(closestDist)
                    if not fin:
                        raise Exception("Invalid Move: No Movements Possible")
                else:
                    break
                count = count + 1

        except Exception as e:
            print(str(e))
            if(count > 0):
                print(str(count) + " " + str(missions))
            pass

        print("Missions: "+str(missions) + " waypoints: "+str(count) + " SampleGI: "+str(statistics.mean(fieldMap)))
        mean = statistics.mean(fieldMap)
        means.append(mean)

        #writeIms(visited, imagedata)
        #writeUtil(visited, imagedata)
        writeUtilAst(visited, imagedata, prof)
        #writeHDFS()
        '''
        for v in range(0, len(visited)):
            row = int(visited[v].split(',')[0][1:])
            col = int(visited[v].split(',')[1][0:-1])
            try:
                curMap[row][col] = fieldMap[v]
            except:
                pass #row,col is not in range of sample space
        '''
    print("Mean GI:" + str(mean))
    writeEnergy(energy)
    print('Energy: '+str(energy))
