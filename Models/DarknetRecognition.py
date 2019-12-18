from pydarknet import Detector, Image
#from PIL import Image
import cv2
import argparse
import os
import time
import sys
import contextlib
'''
Nat Shineman
11OCT2019
GPU version of YOLOv3 opject recognition
using yoloface for weights
'''
#YOLO = 'yolov3'
YOLO = 'yoloface'
# absolute path for testing
# YOLO = '/home/SoftwarePilot/Models/yoloface'
YOLO = os.path.join(os.environ['AUAVHOME'], 'Models/yoloface')
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

# again wget'ing from google docsis terrible
if(not os.path.exists(os.path.join(YOLO, 'yolov3.weights'))):
    os.system('''wget --load-cookies /tmp/cookies.txt -r "https://docs.google.com/uc?export=download&confirm=$(wget --quiet --save-cookies /tmp/cookies.txt --keep-session-cookies --no-check-certificate 'https://docs.google.com/uc?export=download&id=13gFDLFhhBqwMw6gf8jVUvNDH2UrgCCrX' -O- | sed -rn 's/.*confirm=([0-9A-Za-z_]+).*/\1\n/p')&id=13gFDLFhhBqwMw6gf8jVUvNDH2UrgCCrX" -O yoloface/yoloface.weights.zip && rm -rf /tmp/cookies.txt''')
    os.system('rm -rf /tmp/cookies.txt')
    os.system('unzip -q yoloface/yoloface.weights.zip -d yoloface')
    os.system('mv yoloface/yolov3-wider_16000.weights yoloface/yolov3.weights')
    
weights = os.path.join(YOLO, 'yolov3.weights')
cfg = os.path.join(YOLO, 'yolov3.cfg')
classfile = os.path.join(YOLO, 'yolo.data')
#imgPath = 'test.jpg'
imgPath = '/tmp/pictmp.jpg'
# replace with optional args
if args.image:
    imgPath = args.image
img = cv2.imread(imgPath)

with contextlib.redirect_stdout(None):
    net = Detector(bytes(cfg, encoding='utf-8'), bytes(weights, encoding='utf-8'), 0, bytes(classfile, encoding='utf-8'))
    darknetImg = Image(img)
    startTime = time.time()
    results = net.detect(darknetImg)
    endTime = time.time()
# remove this for properly formatted output
# print("Image Processed, took {:.6f} seconds".format(endTime - startTime))

for cat, score, bounds in results:
    # print box location and draw in image
    x, y, w, h = bounds
    # print("{} {}% : [ {} {} {} {} ]".format(str(cat.decode('utf-8')), int(score*100), int(x), int(y), int(w), int(h)))
    #print("{} {}% : [ {} {} {} {} ]".format(str(cat.decode('utf-8')), int(score*100), int(x - w / 2), int(y - h /2), int(x + w / 2), int(y + h / 2)))

    print("[ {} {} {} {} ]".format(int(x - w / 2), int(y - h /2), int(x + w / 2), int(y + h / 2)))
    
    cv2.rectangle(img, (int(x - w / 2), int(y - h /2)), (int(x + w / 2), int(y + h / 2)), (255, 0, 0), thickness = 2)
    cv2.putText(img, str(cat.decode('utf-8')), (int(x), int(y)), cv2.FONT_HERSHEY_COMPLEX, 1, (255, 255, 0))

#cv2.imshow("output", img)
#cv2.waitKey()

cv2.imwrite("obj-detect-darknet.jpg", img)

#cv2.destroyAllWindows()
