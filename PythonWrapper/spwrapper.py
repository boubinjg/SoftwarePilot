from coapthon.client.helperclient import HelperClient
from scp import SCPClient
from paramiko import SSHClient, AutoAddPolicy
import time
import subprocess
import os
import numpy as np
from sklearn.neighbors import NearestNeighbors
#port and path for all SP driver/routine calls
PORT = 5117
PATH = '/cr'
MODEL_PATH = './Models'
MODEL_NUM = 8
K = 9

class Aircraft:
    def __init__(self, ip):
        self.ip = ip
        self.__loadModels()
        self.__readModels()
        self.__readWeights()
        self.__prepDir()
        #TODO select start waypoints
        #self.__call_driver('dn=MissionDriver-dc=init-dp=-dp=-dp=', 2)

    def captureImage(self):

        self.__call_driver('dn=CaptureImageV2Driver-dc=ssm', 1)
        self.__call_driver('dn=CaptureImageV2Driver-dc=get', 2)
        self.__call_driver('dn=CaptureImageV2Driver-dc=get', 2)
        self.__call_driver('dn=CaptureImageV2Driver-dc=dldFull', 5)
        self.__downloadImg()
        return

    def flyToWaypoint(self, lat, lon, alt):
        #This still needs to be tested!
        self.__call_driver('dn=MissionDriver-dc=stopMission', 1)
        self.__call_driver('dn=MissionDriver-dc=initWaypoint-dp='+str(lat)+'-dp='+str(lon)+'-dp='+str(alt), 1)
        self.__call_driver('dn=MissionDriver-dc=uploadMission', 1)
        self.__call_driver('dn=MissionDriver-dc=startMission', 5)

    def extractFeatures(self):
        cwd = os.getcwd()
        result = subprocess.run(['Parser/runParser.sh', cwd+'/tmp/img.JPG'], stdout=subprocess.PIPE)
        features = result.stdout.decode("utf-8").strip()[1:-1].split(",")
        features = [float(x.strip().split('=')[1]) for x in features]
        npfeatures = np.array(features)
        return npfeatures

    def getQVals(self, featureVector):
        qv = [0,0,0,0]
        for i in range(MODEL_NUM):
            model_qv = self.__getQVals(self.knndataset[i], featureVector)
            print(model_qv)
            print(self.weights)
            model_qv = [x*self.weights[i] for x in model_qv]
            qv = [x+y for x,y in zip(qv, model_qv)]
        qv = [x/MODEL_NUM for x in qv]

        return qv

    def __call_driver(self, cmd, sleepTime):
        print('In')
        client = HelperClient(server=(self.ip, PORT))
        response = client.put(PATH, cmd)
        print('sleep')
        time.sleep(sleepTime)
        print('out')
        client.stop()

    def __downloadImg(self):
        client = SSHClient()
        client.load_system_host_keys()
        client.set_missing_host_key_policy(AutoAddPolicy())
        client.connect(
            self.ip,
            username='ssh',
            password='ssh',
            timeout=5000,
            port=22222,
        )
        scp = SCPClient(client.get_transport())
        scp.get('/storage/emulated/0/AUAVtmp/fullPic.JPG', 'tmp/img.JPG')
        return 0

    def __readModels(self):
        self.knndataset = []
        for i in range(MODEL_NUM):
            f = "{0:04b}".format(i)
            ds = np.genfromtxt(MODEL_PATH+'/'+f,delimiter=',')
            self.knndataset.append(ds)

    def __loadModels(self):
        for i in range(MODEL_NUM):
            f = "{0:04b}".format(i)
            out = os.popen('/opt/hadoop/bin/hadoop fs -test -e hdfs://master:9820/model_'+f+'/knndata && echo $?').read()
            print(out)
            try:
                if(int(out) == 0):
                    print(f + ' found')
                    os.remove('Models/'+f)
                    subprocess.call(['/opt/hadoop/bin/hadoop','fs','-get','hdfs://master:9820/model_'+f+'/knndata', 'Models/'+f])
                else:
                    print('not found')
            except:
                print(f + ' not found')

    def __getQVals(self, model, features):
        nbors = NearestNeighbors(K)
        nbors.fit(model[:,0:12])

        knn = nbors.kneighbors([features[0:12]])
        knn = knn[1][0]
        dirs = [[0,0],[0,0],[0,0],[0,0]]

        for n in knn:
            ug = model[n][13:17]

            for d in range(0,4):
                dirs[d][0] += 1
                dirs[d][1] += ug[d]
        mapGain = [d[1]/(d[0]-.00001) for d in dirs]
        return mapGain

    def __readWeights(self):
        with open('../DataAnalysis/weights') as f:
            lines = f.readlines()
            self.weights = lines[0].split(',')
            self.weights = [float(x) for x in self.weights]
    def __prepDir(self):
        dir = 'tmp/export/'
        for f in os.listdir(dir):
            os.remove(os.path.join(dir, f))
