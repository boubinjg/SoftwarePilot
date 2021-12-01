# NOTE: This only works for JPG image whoch provide exifdata. Doesn't work for PNG images.
# This is a script to extract GPS co-ordinates from the image.
# We will be usin g Python's Pillow library to achieve this.

from PIL import Image
from PIL.ExifTags import TAGS, GPSTAGS


class ImageMetaData(object):

    #Extract the exif data from any image. Data includes GPS coordinates,
    #Focal Length, Date of modification, and all relevant information we would want.

    exif_data = None
    image = None

    # Like a constructor for the class.
    def __init__(self, img_path):
        self.image = Image.open(img_path)
        #print(self.image._getexif())
        self.get_exif_data() #get all the exif data of the image
        super(ImageMetaData, self).__init__()

    #function to get the entire exif_data
    def get_exif_data(self):
        #Returns a dictionary from the exif data of Image.
        exif_data = {}
        info = self.image._getexif() #Just a call to _getexif() would yield all info.
        #print(info)
        if info:
            for tag, value in info.items():
                decoded = TAGS.get(tag, tag)
                if decoded == "GPSInfo":
                    gps_data = {}
                    for t in value:
                        sub_decoded = GPSTAGS.get(t, t)
                        gps_data[sub_decoded] = value[t]

                    exif_data[decoded] = gps_data
                else:
                    exif_data[decoded] = value
        self.exif_data = exif_data
        return exif_data

    def get_if_exist(self, data, key):
        if key in data:
            return data[key]
        return None

    def convert_to_degress(self, value):
        #print(value)
        # function to convert gps to degrees
        d0 = value[0][0]
        d1 = value[0][1]
        d = float(d0) / float(d1)

        m0 = value[1][0]
        m1 = value[1][1]
        m = float(m0) / float(m1)

        s0 = value[2][0]
        s1 = value[2][1]
        s = float(s0) / float(s1)

        val = d+(m/60.0) +(s/3600.0)
        #print(val)
        return d + (m / 60.0) + (s / 3600.0)

    def get_lat_lng(self):
        #Returns the latitude and longitude
        lat = None
        lng = None
        exif_data = self.get_exif_data()
        #print(exif_data)
        if "GPSInfo" in exif_data:
            gps_info = exif_data["GPSInfo"]
            gps_latitude = self.get_if_exist(gps_info, "GPSLatitude")
            gps_latitude_ref = self.get_if_exist(gps_info, 'GPSLatitudeRef')
            gps_longitude = self.get_if_exist(gps_info, 'GPSLongitude')
            gps_longitude_ref = self.get_if_exist(gps_info, 'GPSLongitudeRef')
            if gps_latitude and gps_latitude_ref and gps_longitude and gps_longitude_ref:
                lat = self.convert_to_degress(gps_latitude)
                if gps_latitude_ref != "N":
                    lat = 0 - lat
                lng = self.convert_to_degress(gps_longitude)
                if gps_longitude_ref != "E":
                    lng = 0 - lng
        #print(lat)
        #print(lng)
        return lat, lng
