import csv
import random
import numpy as np
from extrapolate import extrapolate
'''
* The WaypointSet Class reads in an application-specific csv waypoint file composed of 81
* waypoints and provides a structured way to associate waypoints for the UAV.
*
* Only the first 81 lines of the file are read.
* The format of each of those lines should be as follows:
* lat,lon,alt
*
* Spatially, the waypoints should represent a 9x9 square, where the first waypoint in the file
* is the northwest most waypoint, and the lat waypoint is the southeast most waypoint.
* Other waypoints should be structured in the following grid pattern:
*  1,  2,  3,  4,  5,  6,  7,  8,  9,
* 10, 11, 12, 13, 14, 15, 16, 17, 18,
* ...
* 73, 74, 75, 76, 77, 78, 79, 80, 81,
'''
dirDict = {0: [[], [1,3], [], [3,1]],
           1: [[], [1,4], [2,0],[3,2]],
           2: [[], [1,5], [2,1], []],
           3: [[0,0], [1,6], [2,4], []],
           4: [[0,1], [1,7], [2,5], [3,3]],
           5: [[0,2], [1,8], [], [3,4]],
           6: [[0,3], [], [2,7], []],
           7: [[0,4], [], [2,8], [3,6]],
           8: [[0,5], [], [], [3,7]],
            }


class WaypointSet:
    def __init__(self, wpfile):
        self.file = wpfile
        self.readWpFile()
        self.sgList = []
        self.buildSGs()

    def readWpFile(self):
        self.wplist = []
        with open(self.file) as csvfile:
            reader = csv.reader(csvfile, delimiter=',')
            for row in reader:
                wp = Waypoint(row[0], row[1])
                self.wplist.append(wp)

    def buildSGs(self):
        for i in range(9):
            sgWaypoints = []
            base = i//3 * 18 + i*3
            for j in range(3):
                rbase = base + j*9
                for k in range(3):
                    sgWaypoints.append(self.wplist[rbase+k])

            currentSG = StateGroup(sgWaypoints)
            self.sgList.append(currentSG)

    def getSG(self, num):
        return self.sgList[num]

    def buildMap(self):
        fieldMap = np.zeros((9,9))
        for i in range(len(self.wplist)):
            row = i//9
            col = i%9
            fieldMap[row][col] = self.wplist[i].value

        return extrapolate(fieldMap)

    def saveNeighborUtilities(self, fieldMap):
        for i in range(len(self.wplist)):
            val = self.wplist[i].value
            if(val != -1):
                row = i//9
                col = i%9
                nbors = [[-1,0],[1,0],[0,1],[0,-1]]
                neighborUtils = [1,1,1,1]
                for n in range(4):
                    nrow = row+nbors[n][0]
                    ncol = col+nbors[n][1]
                    if(nrow < 0 or nrow >= 9 or ncol < 0 or ncol >= 9):
                        pass
                    else:
                        neighborUtils[n] = fieldMap[row+nbors[n][0]][col+nbors[n][1]]

                row = i//27
                col = i%9 / 3
                SG = int(row * 3 + col)

                base = SG//3 * 18 + SG * 3
                irow = (i-base)//9
                icol = (i-base) % 3

                wpNum = irow*3 + icol

                print(neighborUtils)
                fname = 'tmp/export/IMG_'+str(SG)+'_'+str(wpNum)+'.CSV'
                with open(fname, 'w') as myfile:
                    wr = csv.writer(myfile, quoting=csv.QUOTE_MINIMAL)
                    wr.writerow(neighborUtils)


class StateGroup:
    def __init__(self, sgWaypoints):
        self.wplist = sgWaypoints
        self.visited = []
    def markVisited(self, num):
        self.visited.append(num)
    def getStart(self):
        return self.wplist[0], 0
    def getWPByNum(self, num):
        return self.wplist[num]
    def getNextWaypoint(self, qvals, wpNum):
        print(self.visited)
        qpairs = []
        for i in range(4):
            qpairs.append([i, qvals[i]])
        sorted_qpairs = sorted(qpairs, key=lambda x: x[1], reverse=True)
        for i in range(4):
            candidate = sorted_qpairs[i]
            returnWPNum = self.__dir_to_wp(candidate, wpNum)
            if(returnWPNum != None):
                return self.getWPByNum(returnWPNum), returnWPNum

        return self.visitRandom()

    def __dir_to_wp(self, candidate, wpNum):
        if(dirDict[wpNum][candidate[0]] != []):
            nextWPNum = dirDict[wpNum][candidate[0]][1]
            if(nextWPNum not in self.visited):
                return nextWPNum
            else:
                print('visited')
        return None

    def visitRandom(self):
        print('Rand')
        rand = list(range(9))
        random.shuffle(rand)
        for i in rand:
            if i not in self.visited:
                return self.getWPByNum(i), i

class Waypoint:
    def __init__(self, lat, lon):
        self.latitude = lat
        self.longitude = lon
        #self.altitude = alt
        self.value = -1

    def setValue(self, value):
        self.value = value
