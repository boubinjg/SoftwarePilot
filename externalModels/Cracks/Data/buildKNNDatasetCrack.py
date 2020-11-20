import sys
import csv
import subprocess

dataset = []
with open(sys.argv[1], 'r') as data:
    reader = csv.reader(data)
    dataset.append(list(reader))

#Format for neighbors:
# ['file that contains neighbor', 'neighbor index']
# EG: ['Photos_16SEP2018_134', '[0,0]']

knndata = []
count = 0
for ds in dataset:
    for i in range(len(ds)):
        line = ds[i]
        reference = ""
        if(len(line[12].split(',')) == 15):
            ref = line[12][1:-1]
            for val in ref.split(','):
                reference += val.split('=')[1] + ","
            #knndata.append(reference[:-1].split(','))

            curUtil = float(line[12].split(',')[14][:-1].split('=')[1])
            for j in (line[4:8]):
                util = 0
                if(j != '[]'):
                    util = float(ds[int(j)-1][12].split(',')[14][:-1].split('=')[1])
                else:
                    pass
                print(util)
                reference += str(util/curUtil) + ','
            list = reference[:-1].split(',')
            print(list)
            for i in range(len(list)):
                if(list[i] == 'None'):
                    list[i] = '0'
            print(list)
            knndata.append(list)
            
with open('knndataset', 'w+') as of:
	wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
	for line in knndata:
		wr.writerow(line)
