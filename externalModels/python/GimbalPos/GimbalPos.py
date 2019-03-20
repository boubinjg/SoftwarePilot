import sys
res=[]
file_path = sys.argv[1] #retriev the file_name/path_to_file name specified through command line
with open(file_path,'rt') as myfile:
    for line in myfile:
        #print(line)
        res.append(line.strip("{}"))
#print(res[0])

list = res[0].split(',')
#print("list=")
#print(list)
#print(list[5])

angle = list[5].split(':')[1].strip('\"')
print("GimbalPosition="+str(angle/30.0))

