import argparse
from PIL import Image, ImageStat
import math

parser = argparse.ArgumentParser()
parser.add_argument('fname')
parser.add_argument('top')
parser.add_argument('right')
parser.add_argument('bottom')
parser.add_argument('left')
parser.add_argument('pref', default="", nargs="?")
args = parser.parse_args()

def brightnessOfMask(im, l, t, r, b):
    mask = im.crop((int(l), int(t), int(r), int(b)))
    stat = ImageStat.Stat(mask)
    r,g,b = stat.mean
    brightness = math.sqrt(0.241*(r**2) + 0.691*(g**2) + 0.068*(b**2))
    return brightness

def brightnessOfRest(im, l, t, r, b):
    imWidth, imHeight = im.size
    tm = brightnessOfMask(im, 0, 0, imWidth, int(t))
    st = imWidth * int(t)
    bm = brightnessOfMask(im, 0, int(b), imWidth, imHeight)
    sb = (imHeight-int(b))*imWidth
    lm = brightnessOfMask(im, 0, int(t), int(l), int(b))
    sl = int(l)*(int(b)-int(t))
    rm = brightnessOfMask(im, int(r), int(t), imWidth, int(b))
    sr = (imWidth-int(r))*(int(b)-int(t))

    return (tm*st+bm*sb+lm*sl+rm*rm)/(st+sb+sl+sr)

im = Image.open(args.fname)
imWidth, imHeight = im.size
top = args.top
left = args.left
if(top == "0"):
    top = "1"
if(left == "0"):
    left = "1"

if args.left == "-1":
    brightness = -255
else:
    brightness = brightnessOfMask(im, left, top, args.right, args.bottom)
if args.left == "-1":
    brightnessOfRest = -255
else:
    brightnessOfRest = brightnessOfRest(im, left, top, args.right, args.bottom)

totalBrightness = brightnessOfMask(im, 0, 0, imWidth, imHeight)
p = args.pref
print(p+"BrightnessOfMask="+str(brightness/255.0))
print(p+"BrightnessOfRest="+str(brightnessOfRest/255.0))
print(p+"BrightnessTotal="+str(totalBrightness/255.0))
