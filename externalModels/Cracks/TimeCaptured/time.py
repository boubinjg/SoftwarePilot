import sys

#path_name = '/Users/naveentr/Drone_codebase/feature_extraction/DJI_1957.JPG'

path_name = sys.argv[1] #get the image name/path_to_image from the user.
time = "0"
with open(path_name, 'r') as fp:
    for line in fp:
        if line.strip().split('=')[0] == "time":
            time = line.strip().split('=')[1]
#meta_data =  ImageMetaData(path_name)
#latlng = meta_data.get_lat_lng()
print("time="+time)
#print(meta_data)
