# Usage python3 gimbalDegree.py SelfieCube20_C2_15.JPG
# It extracts 20 (i.e Global_Img_count) and returns the appropriate angle)

import sys
from os.path import basename

im = sys.argv[1]

Img_name = basename(im)
list = Img_name.split('_')


Img_global_Cube_count = int(list[0].strip('SelfieCube'))

#print(Img_global_Cube_count)
#Img_global_Cube_count = 19

GimbalPos = [0,-15,-30] #The possible Gimbal positions stored in a list
DroneSlice = ['C','D','U'] #The possible Drone Slices stored in a list


y_axis = 1

#Based on the below check, y-axis value would be suitably updated.
if Img_global_Cube_count <= 24 and Img_global_Cube_count >=1:
    y_axis +=0
elif Img_global_Cube_count <= 48 and Img_global_Cube_count >=25:
    y_axis -=1
else:
    y_axis +=1

#################################################################
#								#
# Logic to find the Drone Position in a 1X3X1 Cube 		#
# Update the y-axis at first.					#
# X-axis and Z-axis can take 0 or 1 only.			#
# Y-axis can be 0 or 1 or 2, so updating this is crucial.	#
# Based on this Drone position can be identified as follows:	#
# 1 to 6 =   (1,1,1)						#
# 7 to 12 =  (0,1,1)						#
# 13 to 18 = (0,1,0)						#
# 19 to 24 = (1,1,0) 						#
# This repeates again in bottom and upper slices of cube        #
#################################################################

Drone_Cube_pos = [] #Initialize a list to contain Drone Position in X-Y-Z axis
count = Img_global_Cube_count%24

#Initialize teh values for x-axis and z-axis
x_axis = 0
z_axis = 0

gimbal_degree = 0

if count >=1 and count<= 6:
    x_axis = 1
    z_axis = 1
elif count >= 7 and count<=12:
    x_axis = 0
    z_axis = 1
elif count >= 13 and count <= 18:
    x_axis = 0
    z_axis = 0
else:
    x_axis = 1
    z_axis = 0

count %= 6
if count <= 2:
    gimbal_degree = 0
elif count <= 4:
    gimbal_degree = -15
else:
    gimbal_degree = -30

Drone_Cube_pos.append(x_axis)
Drone_Cube_pos.append(y_axis)
Drone_Cube_pos.append(z_axis)

print("X="+str(x_axis))
print("Y="+str(y_axis))
print("Z="+str(z_axis))
print("GimbalPos="+str(gimbal_degree))
#return Drone_Cube_pos
