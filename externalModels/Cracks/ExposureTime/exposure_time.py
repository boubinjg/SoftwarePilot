# This is a function to extract teh exposure time from image
from PIL import Image
from PIL.ExifTags import TAGS

def get_exif(fn):
    ret = {} #Initilise ret to null
    i = Image.open(fn) #Open the image
    info = i._getexif() #Extract the metadata from the image.
    for tag, value in info.items():
        decoded = TAGS.get(tag, tag) #This gets all the tags and value
        #print(decoded)
        ret[decoded] = value
    return ret['ExposureTime'] #Return just the exposure time
