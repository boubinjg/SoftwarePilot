from pydarknet import Detector, Image
#from PIL import Image
import cv2
import argparse
import os
import time

'''
Nat Shineman
11OCT2019
GPU version of YOLOv3 opject recognition
'''
YOLO = 'yolov3'

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
if(not os.path.exists(os.path.join(YOLO, 'yolov3.weights'))):
    os.system('wget https://pjreddie.com/media/files/yolov3.weights -P /home/SoftwarePilot/Models/yolov3')
weights = os.path.join(YOLO, 'yolov3.weights')
cfg = os.path.join(YOLO, 'yolov3.cfg')
classfile = os.path.join(YOLO, 'coco.data')
imgPath = 'test.jpg'

# replace with optional args
if args.image:
    imgPath = args.image
img = cv2.imread(imgPath)

net = Detector(bytes(cfg, encoding='utf-8'), bytes(weights, encoding='utf-8'), 0, bytes(classfile, encoding='utf-8'))

darknetImg = Image(img)

startTime = time.time()
results = net.detect(darknetImg)
endTime = time.time()
print("Image Processed, took {:.6f} seconds".format(endTime - startTime))

for cat, score, bounds in results:
    # print box location and draw in image
    x, y, w, h = bounds
    # print("{} {}% : [ {} {} {} {} ]".format(str(cat.decode('utf-8')), int(score*100), int(x), int(y), int(w), int(h)))
    print("{} {}% : [ {} {} {} {} ]".format(str(cat.decode('utf-8')), int(score*100), int(x - w / 2), int(y - h /2), int(x + w / 2), int(y + h / 2)))
    cv2.rectangle(img, (int(x - w / 2), int(y - h /2)), (int(x + w / 2), int(y + h / 2)), (255, 0, 0), thickness = 2)
    cv2.putText(img, str(cat.decode('utf-8')), (int(x), int(y)), cv2.FONT_HERSHEY_COMPLEX, 1, (255, 255, 0))

#cv2.imshow("output", img)
#cv2.waitKey()

cv2.imwrite("obj-detect-darknet.jpg", img)

#cv2.destroyAllWindows()
