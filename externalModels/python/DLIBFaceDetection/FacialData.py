import subprocess
from PIL import Image
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('fname')
args = parser.parse_args();
#get face box result from dlib, trim newline character, convert from byte to string
detection = (subprocess.check_output(["face_detection", args.fname])[:-1]).decode("utf-8")
#convert string list to integers, remove first (file name)
box = list(map(int, detection.split(',')[1:]))
if box == []:
    print("width="+str(-1))
    print("height="+str(-1))
    print("centerx="+str(-1))
    print("centery="+str(-1))
    print("top="+str(-1))
    print("right="+str(-1))
    print("bottom="+str(-1))
    print("left="+str(-1))
else:
    top = box[0];
    right = box[1];
    bottom = box[2];
    left = box[3];
    centerx = (right+left)/2
    centery = (top+bottom)/2

    im = Image.open(args.fname)
    imWidth, imHeight = im.size
    boxWidth = right-left
    boxHeight = bottom-top

    print("width="+str(boxWidth/imWidth))
    print("height="+str(boxHeight/imHeight))
    print("centerx="+str(centerx/imWidth))
    print("centery="+str(centery/imHeight))
    print("top="+str(top))
    print("right="+str(right))
    print("bottom="+str(bottom))
    print("left="+str(left))

