from PIL import Image
import os
import argparse
import cv2
import numpy as np
import time

'''
Nat Shineman
26SEP2019
YOLOv3 Implementation of image detection 
'''
# constants for filtering
CONFIDENCE = 0.5
NMS_SUPPRESS = 0.4
# YOLOv3 path 
#YOLO = '/home/SoftwarePilot/Models/yolov3'
#YOLO = 'yolov3'
YOLO = 'yoloface'

# TODO: add GPU toggle
def getOutputLayers(net):
    layerNames = net.getLayerNames()
    outputLayers = [layerNames[i[0] - 1] for i in net.getUnconnectedOutLayers()]
    return outputLayers

def drawBoundingBox(img, classID, conf, x, y, xMax, yMax):
    label = str(classes[classID])
    color = COLORS[classID]
    cv2.rectangle(img, (x, y), (xMax, yMax), color, 2)
    cv2.putText(img, label, (x-10, y-10), cv2.FONT_HERSHEY_SIMPLEX, .5, color, 2)

# command line args for testing
argp = argparse.ArgumentParser()
argp.add_argument('-i', '--image', help = 'path to input image')
'''
argp.add_argument('-c', '--config', help = 'path to yolo config file')
argp.add_argument('-w', '--weights', help = 'path to pre-trained weights')
argp.add_argument('-cl', '--classes', help = 'path to classes text file')
'''
args = argp.parse_args()

# default values and import weights
#if(not os.path.exists(os.path.join(YOLO, 'yolov3.weights'))):
    #os.system('wget https://pjreddie.com/media/files/yolov3.weights -P /home/SoftwarePilot/Models/yolov3')
#if(not os.path.exists(os.path.join(YOLO, 'yoloface.weights'))):
 #   gdown.download('wget https://drive.google.com/uc?id=pQqb&id=1xYasjU52whXMLT5MtF7RCPQkV66993oR', 'yoloface.weights', quiet=True)

# wget'ing from google docs is far from ideal
if(not os.path.exists(os.path.join(YOLO, 'yolov3.weights'))):
    os.system('''wget --load-cookies /tmp/cookies.txt -r "https://docs.google.com/uc?export=download&confirm=$(wget --quiet --save-cookies /tmp/cookies.txt --keep-session-cookies --no-check-certificate 'https://docs.google.com/uc?export=download&id=13gFDLFhhBqwMw6gf8jVUvNDH2UrgCCrX' -O- | sed -rn 's/.*confirm=([0-9A-Za-z_]+).*/\1\n/p')&id=13gFDLFhhBqwMw6gf8jVUvNDH2UrgCCrX" -O yoloface/yoloface.weights.zip && rm -rf /tmp/cookies.txt''')
    os.system('rm -rf /tmp/cookies.txt')
    os.system('unzip -q yoloface/yoloface.weights.zip -d yoloface')
    os.system('mv yoloface/yolov3-wider_16000.weights yoloface/yolov3.weights')

weights = os.path.join(YOLO, 'yolov3.weights')
cfg = os.path.join(YOLO, 'yolov3.cfg')
classfile = os.path.join(YOLO, 'class.names')
#imgPath = '/home/SoftwarePilot/Models/test.jpg'
imgPath = 'test.jpg'
# replace with optional args
if args.image:
    imgPath = args.image
image = cv2.imread(imgPath)

# get image dimensions for later
imWidth = image.shape[1]
imHeight = image.shape[0]
scale = 0.00392

classes = None
with open(classfile, 'r') as inFile:
    classes = [line.strip() for line in inFile.readlines()]

COLORS = np.random.uniform(0, 255, size=(len(classes), 3))

# read pre trained weights and config
net = cv2.dnn.readNetFromDarknet(cfg, weights)

#create input blob
blob = cv2.dnn.blobFromImage(image, scale, (416,416), (0,0,0), True, crop=False)

net.setInput(blob)
startTime = time.time()
outLayers = net.forward(getOutputLayers(net))
endTime = time.time()
print("Image Processed, took {:.6f} seconds".format(endTime - startTime))

# declare output arrays
classIds = []
confidences = []
boxes = []

# loop through output layers and remove low confidence images
for layer in outLayers:
    for detect in layer:
        scores = detect[5:]
        classId = np.argmax(scores)
        conf = scores[classId]
        if conf > CONFIDENCE:
            newBox = detect[0:4] * np.array([imWidth, imHeight, imWidth, imHeight])
            (centerX, centerY, width, height) = newBox.astype("int")
            x = int(centerX - width / 2)
            y = int(centerY - height / 2)
            classIds.append(classId)
            confidences.append(float(conf))
            boxes.append([x, y, int(width), int(height)])

# non max suppression
indicies = cv2.dnn.NMSBoxes(boxes, confidences, CONFIDENCE, NMS_SUPPRESS)
for i in indicies:
    i = i[0]
    box = boxes[i]
    (x, y, w, h) = box[:4]
    # print bounding box and draw in image
    print("{} {:.0f}% : {}".format(classes[classIds[i]], confidences[i] * 100, box))
    drawBoundingBox(image, classIds[i], confidences[i], round(x), round(y), round(x + w), round(y + h))

# display output - for GUI display only
#cv2.imshow("object detection", image)
# wait for key press
#cv2.waitKey()

# save image to disk
cv2.imwrite("obj-detect.jpg", image)

# release windows - for GUI display only
#cv2.destroyAllWindows()
