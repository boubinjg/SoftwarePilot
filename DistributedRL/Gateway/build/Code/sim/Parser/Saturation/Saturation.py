import argparse
from PIL import Image, ImageStat
import math

parser = argparse.ArgumentParser()
parser.add_argument('fname')
parser.add_argument('pref', default="", nargs="?")
args = parser.parse_args()

def SaturationOfMask(im, l, t, r, b):
    mask = im.crop((int(l), int(t), int(r), int(b)))
    stat = ImageStat.Stat(mask)
    r,g,b = stat.mean
    return (r/255,g/255,b/255)

def SaturationOfRest(im, l, t, r, b):
    imWidth, imHeight = im.size
    tm = SaturationOfMask(im, 0, 0, imWidth, int(t))
    st = float(imWidth * int(t))
    bm = SaturationOfMask(im, 0, int(b), imWidth, imHeight)
    sb = float((imHeight-int(b))*imWidth)
    lm = SaturationOfMask(im, 0, int(t), int(l), int(b))
    sl = float(int(l)*(int(b)-int(t)))
    rm = SaturationOfMask(im, int(r), int(t), imWidth, int(b))
    sr = float((imWidth-int(r))*(int(b)-int(t)))
    return ((tm[0]*st+bm[0]*sb+lm[0]*sl+rm[0]*sr)/(st+sb+sl+sr),
            (tm[1]*st+bm[1]*sb+lm[1]*sl+rm[1]*sr)/(st+sb+sl+sr),
            (tm[2]*st+bm[2]*sb+lm[2]*sl+rm[2]*sr)/(st+sb+sl+sr))


im = Image.open(args.fname)
imWidth, imHeight = im.size

totalSaturation = SaturationOfMask(im, 0, 0, imWidth, imHeight)

p=args.pref

print("SaturationTotalRed="+str(totalSaturation[0]))
print("SaturationTotalGreen="+str(totalSaturation[1]))
print("SaturationTotalBlue="+str(totalSaturation[2]))
