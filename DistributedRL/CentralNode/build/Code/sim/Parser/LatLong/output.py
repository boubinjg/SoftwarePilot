from gps_lat_long import *
import sys

#path_name = '/Users/naveentr/Drone_codebase/feature_extraction/DJI_1957.JPG'

path_name = sys.argv[1] #get the image name/path_to_image from the user.
Latitude = 0
Longitude = 0
'''with open(path_name, 'r') as fp:
    for line in fp:
        if line.strip().split('=')[0] == "Latitude":
            Latitude = line.strip().split('=')[1]
        elif line.strip().split('=')[0] == "Longitude":
            Longitude = line.strip().split('=')[1]
'''
meta_data =  ImageMetaData(path_name)
latlng = meta_data.get_lat_lng()
Latitude = latlng[0]
Longitude = latlng[1]
print("Latitude="+str(Latitude))
print("Longitude="+str(Longitude))
