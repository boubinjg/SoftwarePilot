import sys
import csv
import subprocess



dataset = []
for f in sys.argv[1:5]:
	with open(f, 'rb') as data:
		reader = csv.reader(data)
		dataset.append(list(reader))

#Format for neighbors:
# ['file that contains neighbor', 'neighbor index']
# EG: ['Photos_16SEP2018_134', '[0,0]']

knndata = []
count = 0
for ds in dataset:
    for i in range(2, len(ds)/4*3):
        line = ds[i]
        reference = ""
        if(len(line[12].split(',')) == 13):
            ref = line[12][1:-1]
            for val in ref.split(','):
                reference += val.split('=')[1] + ","
            #knndata.append(reference[:-1].split(','))

            for j in (line[13]).split(','):
                print(j)
                reference += j.replace('\'','').replace('[','').replace(']','') + ','
            knndata.append(reference[:-1].split(','))
            print(reference+'\n')



with open('knndatasetGI', 'w+') as of:
	wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
	for line in knndata:
		wr.writerow(line)
