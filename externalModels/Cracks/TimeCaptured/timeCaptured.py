import sys
from capture_time import *

path_name = sys.argv[1]
data = get_exif(path_name)

time = data.split(":")
secs = int(time[0])*3600+int(time[1])*60+int(time[2])
print("time="+str(secs/(24.0*3600)))
