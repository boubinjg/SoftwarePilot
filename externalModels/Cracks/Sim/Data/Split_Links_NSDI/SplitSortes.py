import sys
import csv
import subprocess

dataset = []

def write(data, fname):
    if(len(fname) < 6):
        return
    with open(date+"/"+fname, 'w+') as of:
        wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
        for line in data:
            wr.writerow(line)

f = sys.argv[1]
date = sys.argv[2]

with open(f, 'rb') as data:
	reader = csv.reader(data)
	dataset.append(list(reader))

sorte = ""
path = ""
count = 0;
curData = []
for i in range(1,len(dataset[0])):
    try:
        line = dataset[0][i]
        filepath = line[3].split("/")
        curPath = filepath[6]
        curSorte = filepath[9]
        if(curSorte != sorte):
            print(curSorte)
            write(curData, path+"/"+sorte+".csv")
            sorte = curSorte
            path = curPath
            curData = []
        curData.append(line)

        print([curPath, curSorte])
        count += 1
        print(count)
    except:
        print("Bad Path")

