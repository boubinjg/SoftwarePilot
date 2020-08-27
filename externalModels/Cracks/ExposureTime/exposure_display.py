import sys
from exposure_time import *

path_name = sys.argv[1]
data = get_exif(path_name)

print("exposureTime="+str(data[0]/data[1]))

