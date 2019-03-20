import numpy as np
import cv2 as cv
from PIL import Image
import argparse
import sys
from contextlib import redirect_stdout

parser = argparse.ArgumentParser()
parser.add_argument('fname')
parser.add_argument('prefix', default="", nargs='?')
args = parser.parse_args()

face_cascade = cv.CascadeClassifier('haarcascade_frontalface_default.xml');
img = cv.imread(args.fname)
gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
faces = face_cascade.detectMultiScale(gray, 1.3, 5)

p = args.prefix

if faces == ():
    print(p+"width="+str(-1))
    print(p+"height="+str(-1))
    print(p+"centerx="+str(-1))
    print(p+"centery="+str(-1))
    print(p+"top="+str(-1))
    print(p+"right="+str(-1))
    print(p+"bottom="+str(-1))
    print(p+"left="+str(-1))
else:
    (x,y,w,h) = faces[0]

    top = y
    left = x
    bottom = top + h
    right = left + w

    im = Image.open(args.fname)
    imWidth, imHeight = im.size

    print(p+"width="+str(w/imWidth))
    print(p+"height="+str(h/imHeight))
    print(p+"centerx="+str((left + w/2)/imWidth))
    print(p+"centery="+str((top + h/2)/imHeight))
    print(p+"top="+str(top))
    print(p+"right="+str(right))
    print(p+"bottom="+str(bottom))
    print(p+"left="+str(left))
