import argparse
from PIL import Image, ImageStat
import math
import numpy as np

parser = argparse.ArgumentParser()
parser.add_argument('fname')
parser.add_argument('top')
parser.add_argument('right')
parser.add_argument('bottom')
parser.add_argument('left')
args = parser.parse_args()

def MaskEntropy(im, l, t, r, b):
    mask = im.crop((int(l), int(t), int(r), int(b)))
    N = 5
    greyIm = np.array(mask)
    S = greyIm.shape
    E = np.array(greyIm)
    EL = []
    for row in range(S[0]):
        for col in range(S[1]):
            Lx=np.max([0,col-N])
            Ux=np.min([S[1], col+N])
            Ly=np.max([0, row-N])
            Uy=np.min([S[0],row+N])
            region=greyIm[Ly:Uy,Lx:Ux].flatten()
            EL.append(entropy(region))

    return np.mean(EL)

def entropyOfRest(im, l, t, r, b):
    imWidth, imHeight = im.size
    tm = MaskEntropy(im, 0, 0, imWidth, int(t))
    st = imWidth * int(t)
    bm = MaskEntropy(im, 0, int(b), imWidth, imHeight)
    sb = (imHeight-int(b))*imWidth
    lm = MaskEntropy(im, 0, int(t), int(l), int(b))
    sl = int(l)*(int(b)-int(t))
    rm = MaskEntropy(im, int(r), int(t), imWidth, int(b))
    sr = (imWidth-int(r))*(int(b)-int(t))

    return (tm*st+bm*sb+lm*sl+rm*rm)/(st+sb+sl+sr)

def entropy(signal):
    lensig=signal.size
    symset=list(set(signal))
    numsym=len(symset)
    propab=[np.size(signal[signal==i])/(1.0*lensig) for i in symset]
    ent = np.sum([p*np.log2(1.0/p) for p in propab])
    return ent;

im = Image.open(args.fname).convert('L');
imWidth, imHeight = im.size
if args.left == "-1":
    me = -10
else:
    me = MaskEntropy(im, args.left, args.top, args.right, args.bottom)
print("ContrastOfMask="+str(me/10.0))

if args.left == "-1":
    re = -10
else:
    re = entropyOfRest(im, args.left, args.top, args.right, args.bottom)
print("ContrastOfRest"+str(re/10.0))

te = MaskEntropy(im, 0, 0, imWidth, imHeight)
print("ContrastTotal="+str(te/10.0))
