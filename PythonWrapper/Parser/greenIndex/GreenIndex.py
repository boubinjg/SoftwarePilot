import numpy as np
import argparse
from PIL import Image, ImageStat, ImageMath
import math

parser = argparse.ArgumentParser()
parser.add_argument('fname')
parser.add_argument('pref', default="", nargs="?")
args = parser.parse_args()

im = Image.open(args.fname)
RGB = im.convert('RGB')

imWidth, imHeight = im.size

ratg = 1.2
ratgb = 1.66
ming = 10
ratr = 2
speed = 8

GI = 0
total = 0

for i in range(0, int(imWidth/speed)):
	for j in range(0, int(imHeight/speed)):
		R,G,B = RGB.getpixel((i*speed,j*speed))
		GI += 2*G - R - B
		total = total + 1

print("GreenIndex="+str(float(GI)/total))
