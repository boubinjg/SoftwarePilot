import os
import glob

path = '../Data/'
dirs = ['Worker0_0','Worker0_1','Worker1_1','Worker1_0']

for dir in dirs:
	writeVal = ''
	f = open('Results/'+dir+'.results','w+')
	for result in glob.glob(path+dir+'/*'):
		run = result.split('/')[-1]
		if(run.split('_')[0] == 'run'):
			num = run.split('_')[1]
			runFile = open(result)
			resultValue = runFile.readlines()
			print(resultValue)
			output = resultValue[1].rstrip()+'\n'
			writeVal += num+': '+output
			#print(run)
			print(writeVal)
	f.write(writeVal)
	f.close()
