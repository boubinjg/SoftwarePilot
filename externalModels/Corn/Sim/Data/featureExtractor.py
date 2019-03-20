import sys
import csv
import subprocess

dataset = []
for f in sys.argv[1:2]:
	with open(f, 'rb') as data:
		reader = csv.reader(data)
		dataset.append(list(reader))

#Format for neighbors:
# ['file that contains neighbor', 'neighbor index']
# EG: ['Photos_16SEP2018_134', '[0,0]']

count = 0
for line in dataset[0]:
	fv = subprocess.check_output(['Corn/runParser.sh', str(line[3])])
	line.append(fv.rstrip())
	count = count + 1
	print(str(count) + 'of' + str(len(dataset[0])))

with open(sys.argv[2], 'w+') as of:
	wr = csv.writer(of, quoting=csv.QUOTE_MINIMAL)
	for line in dataset[0]:
		wr.writerow(line)
