import argparse

parser = argparse.ArgumentParser()
parser.add_argument('jsonFile')
args = parser.parse_args()

res=[]
with open(args.jsonFile,'rt') as myfile:
    for line in myfile:
        #print(line)
        res.append(line.strip("{}"))
#print(res[0])

list = res[0].split(',')
#print(list)
#print(list[2])

path_taken = list[2].split(':')[1].strip('\"')
#print(path_taken)

pos = []
for char in path_taken:
    pos += char

#print(pos)

y_dist = 0
# check whether our list contains 'UP' or 'DW'
if 'U'in pos and 'P' in pos:
    #print("UP is present")
    y_dist = 1
    #pos.strip("UP")
    [s.strip('U') for s in pos]
    [s.strip('P') for s in pos]
elif "DW" in pos:
    #print("DW is present")
    y_dist = -1
    #pos.strip("DW")
else:
    #print("Drone in central slice")
    y_dist = 0

mylist = pos
mylist = ' '.join(mylist).replace('U','').replace('P','').replace('_','').split()
#mylist = ' '.join(mylist).replace('P','').split()
#print(mylist)

pos =mylist

'''for char in pos:
    for case in switch(char):
        if case('C'):
           print('C')
            break
        if case('L'):
            print('L')
        if case(): # This is like a default case
            print("This is something else")
'''
#print(len(pos))
n = len(pos)

#print(pos[n-1])
cur_pos = pos[n-1]

cube= []
if cur_pos == 'C':
    cube.append(1)
    cube.append(1+y_dist)
    cube.append(1)
elif cur_pos == 'L':
    cube.append(0)
    cube.append(1+y_dist)
    cube.append(1)
elif cur_pos == 'B':
    cube.append(0)
    cube.append(1+y_dist)
    cube.append(0)
elif cur_pos == 'R':
    cube.append(1)
    cube.append(1+y_dist)
    cube.append(0)
elif cur_pos == 'F':
    cube.append(1)
    cube.append(1+y_dist)
    cube.append(1)

print("X="+str(cube[0]))
print("Y="+str(cube[1]))
print("Z="+str(cube[2]))
#for elem in res:
 ##   print(elem)
