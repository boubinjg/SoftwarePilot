import argparse
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument('fname')
args = parser.parse_args()

im = Image.open(args.fname)
imWidth, imHeight = im.size

print("xResolution="+str(imWidth))
print("yResolution="+str(imHeight))
