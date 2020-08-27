import csv
import random

def readInData(dataset):
    cube = []
    with open(dataset, 'rt', encoding='utf-8') as data:
        reader = csv.reader(data)
        for line in list(reader):
            cube.append(line)
    return cube

cube = readInData('Data/ModifiedNeighborLinksGI2/NeighborLinks_AUG20_new.csv')

total = 0
count = 0
for i in cube:
    try:
        total += float(i[12].split(",")[10].strip().split("=")[1])
    except:
        pass
    count += 1

print(total/count)
