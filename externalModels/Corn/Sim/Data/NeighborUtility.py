import sys
import csv
import subprocess

dataset = []

def getUtil(p, d):
    for line in d[0]:
        if line[1] == p:
            try:
                return line[12].split(',')[10].split('=')[1][:-1]
            except:
                return -1;
    return -1;

for f in sys.argv[1:2]:
	with open(f, 'rb') as data:
		reader = csv.reader(data)
		dataset.append(list(reader))

#Format for neighbors:
# ['file that contains neighbor', 'neighbor index']
# EG: ['Photos_16SEP2018_134', '[0,0]']


count = 0
for line in dataset[0]:
	north = getUtil(line[4], dataset)
	south = getUtil(line[5], dataset)
	east = getUtil(line[6], dataset)
	west = getUtil(line[7], dataset)
	line.append([north, south, east, west])



with open(sys.argv[2], 'w+') as of:
	wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
	for line in dataset[0]:
		wr.writerow(line)

