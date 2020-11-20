import sys
import os
import subprocess
import csv

files = os.listdir(sys.argv[1])
files = sorted(files)

with open(sys.argv[1]+'/meta.txt') as f:
    meta = f.read().splitlines()

metadata = []
for line in meta:
    line = line.split(':')
    line[1] = line[1].strip()
    proc = line[1].split(',')
    for i in range(4):
        if(proc[i] == '-1'):
            proc[i] = []
        else:
            proc[i] = int(proc[i])
    metadata.append(proc)

count = 1

dataset = []

for file in files:
    if(file.endswith('.jpg')):
        line = []
        line.append(5)
        line.append(count)
        line.append(["lat","lon"])
        cwd = os.getcwd()
        line.append(cwd+'/'+sys.argv[1]+'/'+file)
        for i in metadata[count-1]:
            line.append(i)

        line.append([])
        line.append([])
        line.append([]) 
        line.append([])
        
        pwd = os.getcwd()
        os.chdir('../../')

        #fv = subprocess.check_output(['ls',str(sys.argv[1]+'/'+file)])
        fpath = 'Data/CubeImgs/'+sys.argv[1]+'/'+file
        fv = subprocess.check_output(['bash','runParser.sh',fpath])
        
        os.chdir(pwd)
        line.append(fv.rstrip().decode("utf-8")[:-3])
        dataset.append(line)

        count += 1
        print(str(count) + " Out Of "+str(len(files)))

with open(sys.argv[2], 'w+') as of:
    wr=csv.writer(of, quoting=csv.QUOTE_MINIMAL)
    for line in dataset:
        wr.writerow(line)
