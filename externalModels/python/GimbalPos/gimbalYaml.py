import sys

#path_name = '/Users/naveentr/Drone_codebase/feature_extraction/DJI_1957.JPG'

path_name = sys.argv[1] #get the image name/path_to_image from the user.
gp=1
with open(path_name, 'r') as fp:
    for line in fp:
        if line.strip().split('=')[0] == "GimbalPosition":
            gp = int(line.strip().split('=')[1])

#meta_data =  ImageMetaData(path_name)
#latlng = meta_data.get_lat_lng()
print("GimbalPosition="+str(gp/30.0))
#print(meta_data)
