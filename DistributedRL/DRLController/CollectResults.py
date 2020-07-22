import os
import glob
import traceback
import subprocess

path = '../Data/'
dirs = ['Worker0_0','Worker0_1','Worker1_1','Worker1_0']

for dir in dirs:
	writeVal = ''
	f = open('Results/'+dir+'.results','w+')
	writeList = []
	for result in glob.glob(path+dir+'/*'):
		run = result.split('/')[-1]
		print(result)
		try:
			if(run.split('_')[0] == 'run'):
				num = run.split('_')[1]
				runFile = open(result)
				resultValue = runFile.readlines()
				print(resultValue)
				#len = resultValue[0].split(':')[1].rstrip()+' '
				energy = resultValue[1].split(':')[1].rstrip()+','
				#writeVal = energy
				edgeEnergy = resultValue[2].split(':')[1].rstrip()+','
				time = resultValue[3].split(':')[1].rstrip()
				writeVal += num+': '+energy+" "+edgeEnergy+" "+time+'\n'
				#print(run)
				#print(writeVal)
				writeList.append([num, writeVal])		
				writeVal = ''
		except:
			traceback.print_exc()
			pass
		writeList = sorted(writeList, key=lambda x: int(x[0]))
	writeVal = ''
	for i in writeList:
		writeVal += i[1]
	print(writeVal)
	print('writeVal')
	f.write(writeVal)
	f.close()

try:
	subprocess.call(['hadoop', 'fs', '-get', 'hdfs://127.0.0.1:9000/worker0_0/runtime', 'Results/runtime00'])
except:
	pass
try:
	subprocess.call(['hadoop', 'fs', '-get', 'hdfs://127.0.0.1:9000/worker0_1/runtime', 'Results/runtime01'])
except:
	pass
try:
	subprocess.call(['hadoop', 'fs', '-get', 'hdfs://127.0.0.1:9000/worker1_0/runtime', 'Results/runtime10'])
except:
	pass
try:
	subprocess.call(['hadoop', 'fs', '-get', 'hdfs://127.0.0.1:9000/worker1_1/runtime', 'Results/runtime11'])
except:
	pass

