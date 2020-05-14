# import the necessary packages
from keras.preprocessing.image import img_to_array
from keras.models import load_model
import numpy as np
import argparse
import imutils
import pickle
import cv2
import os

ap = argparse.ArgumentParser()
ap.add_argument("-m", "--model", required=True,
	help="path to trained model model")
ap.add_argument("-l", "--labelbin", required=True,
	help="path to label binarizer")
ap.add_argument("-i", "--image", required=True,
	help="path to input image")
args = vars(ap.parse_args())

# load the image
image = cv2.imread(args["image"])
output = image.copy()

# load the trained convolutional neural network and the label
# binarizer
print("[INFO] loading network...")
model = load_model(args["model"])
lb = pickle.loads(open(args["labelbin"], "rb").read())

thumb = cv2.resize(image, (400,300))
#orig ranges were 100,200 / 133,266

# pre-process the image for classification
img = cv2.resize(image, (150, 200))
img = img.astype("float") / 255.0
img = img_to_array(img)
img = np.expand_dims(img, axis=0)

# classify the input image
print("[INFO] classifying image...")
proba = model.predict(img)[0]
idx = np.argmax(proba)
label = lb.classes_[idx]

print("corn="+str(-1*(idx-1)))
