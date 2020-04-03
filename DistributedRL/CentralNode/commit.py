import subprocess

op = subprocess.check_output(["sudo","docker", "ps","-a"])
op = op.split("\n")
for i in op:
    i = i.split()
    try:
        if(i[1] == "spcn"):
            print([i[0], i[1]])
            op = subprocess.check_output(["sudo","docker","commit",i[0], i[1]])
            print(op)
    except:
        pass
