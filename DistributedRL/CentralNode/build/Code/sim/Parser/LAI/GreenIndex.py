import argparse
from PIL import Image, ImageStat
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

leafcount = 0
total = 0
for i in range(0, imWidth/speed):
	for j in range(0, imHeight/speed):
		R,G,B = RGB.getpixel((i*speed,j*speed))
		if R*ratg < G and B*ratgb < G and B*ratr < R:
			leafcount = leafcount + 1
		total = total+1
print("LAI="+str(float(leafcount)/total))
