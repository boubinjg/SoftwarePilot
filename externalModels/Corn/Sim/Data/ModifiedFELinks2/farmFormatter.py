import sys
import csv
import subprocess

dataset = []

f = sys.argv[1]
with open(f, 'rb') as data:
	reader = csv.reader(data)
	dataset.append(list(reader))

#Format for neighbors:
# ['file that contains neighbor', 'neighbor index']
# EG: ['Photos_16SEP2018_134', '[0,0]']

#in order from June to Sep
#offenders = [1,2,32,33,34,35,38,39]
offenders = [1,2,3,4,14,16,32,33,34,35,38,3944,46]
#offenders = [1,2,3,4,14,16,32,33,34,35,38,39,44,46]
#offenders = [1,2,3,4,5,6,18,20,32,33,34,35,38,39]

count = 1
inc = 0
currentRow = 1
lastRow = 1
ndata = []
for i in range(1,len(dataset[0])):
    line = dataset[0][i]

    row = int(line[1].split(",")[0][1:])
    col = int(line[1].split(",")[1][0:-1])

    if row != lastRow and row not in offenders:
        currentRow = currentRow + 1

    if row in offenders:
        #print([row, currentRow])
        pass
    else:
        print([row, currentRow])
        line[1] = "[" + str(currentRow) + "," + str(col) + "]"
        if(line[4] != '[]'):
            line[4] = "[" + str(currentRow-1) + "," + str(col) + "]"
        if(line[5] != '[]'):
            line[5] = "[" + str(currentRow+1) + "," + str(col) + "]"
        if(line[6] != '[]'):
            line[6] = "[" + str(currentRow) + "," + str(col+1) + "]"
        if(line[7] != '[]'):
            line[7] = "[" + str(currentRow) + "," + str(col-1) + "]"

        print(line)
        ndata.append(line)

    #if(line[12] == "[Utility=NaN]"):
    #    print(row)
    count = count + 1
    lastRow = row


with open(sys.argv[2], 'w+') as of:
	wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
	for line in ndata:
		wr.writerow(line)
