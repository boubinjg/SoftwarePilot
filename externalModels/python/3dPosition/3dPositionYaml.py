import sys

#path_name = '/Users/naveentr/Drone_codebase/feature_extraction/DJI_1957.JPG'

path_name = sys.argv[1] #get the image name/path_to_image from the user.
X = "1"
Y = "1"
Z = "1"
with open(path_name, 'r') as fp:
    for line in fp:
        if line.strip().split('=')[0] == "X":
            X = line.strip().split('=')[1]
        elif line.strip().split('=')[0] == "Y":
            Y = line.strip().split('=')[1]
        elif line.strip().split('=')[0] == "Z":
            Z = line.strip().split('=')[1]

#meta_data =  ImageMetaData(path_name)
#latlng = meta_data.get_lat_lng()
print("X="+X)
print("Y="+Y)
print("Z="+Z)

#print(meta_data)
