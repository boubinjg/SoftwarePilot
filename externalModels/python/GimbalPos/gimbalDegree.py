# Usage python3 gimbalDegree.py SelfieCube20_C2_15.JPG
# It extracts 20 (i.e Global_Img_count) and returns the appropriate angle)

import sys
from os.path import basename

Img_name = basename(sys.argv[1])

list = Img_name.split('_')

#print("list=",list)


Img_global_Cube_count = int(list[0].strip('SelfieCube'))

#print(Img_global_Cube_count)
#Img_global_Cube_count = 19

GimbalPos = [0,-15,-30] #The possible Gimbal positions stored in a list

#################################################################
#								#
#Logic to find the Gimbal Position 				#
# current_number%6 gives a number between 1 to 6(incuded).	#
# Based on this Gimbal position can be identified as follows:	#
# 1 or 2 == 0 degree						#
# 3 or 4 == 15 degree						#
# 5 or 6 == 30 degree						#
#								#
#################################################################

gimbal_degree = 0
count = Img_global_Cube_count%6

if count ==1 or count == 2:
    gimbal_degree = 0
elif count == 3 or count ==4:
    gimbal_degree = -15
else:
    gimbal_degree = -30

print("GimbalPosition="+str(gimbal_degree/30.0))
#return gimbal_degree
