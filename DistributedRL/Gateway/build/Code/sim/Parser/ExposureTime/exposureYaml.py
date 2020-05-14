import sys

#path_name = '/Users/naveentr/Drone_codebase/feature_extraction/DJI_1957.JPG'

path_name = sys.argv[1] #get the image name/path_to_image from the user.
exp = "0"
with open(path_name, 'r') as fp:
    for line in fp:
        if line.strip().split('=')[0] == "ExposureTime":
            exp = line.strip().split('=')[1]

#meta_data =  ImageMetaData(path_name)
#latlng = meta_data.get_lat_lng()
print("exposureTime="+exp)
#print(meta_data)
