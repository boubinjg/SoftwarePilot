import sys

f = open(sys.argv[1],'r')
lines = f.readlines()
for i in range(0,1):
	if(i == 0):
		print('UAV Energy')
	elif(i == 1):
		print('Edge Energy')
	elif(i == 2):
		print('Time')
	for line in lines:
		num = line[2:].rstrip().split(',')
		print(num[i].replace(':','').strip())
