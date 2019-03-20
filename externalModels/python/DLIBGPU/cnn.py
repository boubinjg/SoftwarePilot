import face_recognition, os
import PIL.Image
import math
import numpy
import sys
import socket
import time
KILL = False
width = 0
height = 0
#imgs = os.listdir('HomeParkFaces/')
#img_lst = []
#for img in imgs:
#    img_lst.append('HomeParkFaces/'+img)
#    print(img)
def getMasks(img):
    global height, width
    im = PIL.Image.open(img)
    imWidth, imHeight = im.size
    width = imWidth
    height = imHeight
    cw = int(imWidth/3)
    ch = int(imHeight/3)
    masks = []
    ims = im.resize((1323,992), PIL.Image.ANTIALIAS)
    masks.append([numpy.asarray(ims), 3])
    for i in range(0,3):
        for j in range(0,3):
            #print(cw*i, ch*j, cw*(i+1), ch*(j+1))
            cimg = im.crop((cw*i, ch*j, cw*(i+1), ch*(j+1)))
            masks.append([numpy.asarray(cimg), cw*i,ch* j])
    for i in range(0,2):
        for j in range(0,2):
            wbuff = cw/2
            hbuff = ch/2
            cimg = im.crop((cw*i + wbuff, ch*j + hbuff, cw*(i+1) + wbuff, ch*(j+1) + hbuff))
            arr = numpy.asarray(cimg)
            masks.append([arr, cw*i, ch*j])
    return masks
def locate(img):
    located_img = []
    imgmasks = getMasks(img)
    loc = []
    for mask in imgmasks:
        mask[0].flags.writeable = True
        curloc = face_recognition.face_locations(mask[0], model='cnn', number_of_times_to_upsample=1)
        #curloc = face_recognition.face_locations(mask[0])
        if curloc != []:
            if(len(mask)>2):
                loc = [curloc[0][0]+mask[1],
                        curloc[0][1]+mask[2],
                        curloc[0][2]+mask[1],
                        curloc[0][3]+mask[2]]
            else:
                loc = [curloc[0][0]*mask[1],
                        curloc[0][1]*mask[1],
                        curloc[0][2]*mask[1],
                        curloc[0][3]*mask[1]]
            break;
    located_img.append(loc)
    return located_img

#img_arrays = load(img_lst)
#print(len(img_lst))

def readlines(sock, recv_buffer=4096, delim='\n'):
	buffer = ''
	data = True
	while data:
		data = sock.recv(recv_buffer)
		buffer += data

		while buffer.find(delim) != -1:
			line, buffer = buffer.split('\n', 1)
			yield line
	return

def facialData(box):
    global width,height
    ret = ""
    if box == []:
        ret += "width="+str(-1)+'\n'
        ret += "height="+str(-1)+'\n'
        ret += "centerx="+str(-1)+'\n'
        ret += "centery="+str(-1)+'\n'
        ret += "top="+str(-1)+'\n'
        ret += "right="+str(-1)+'\n'
        ret += "bottom="+str(-1)+'\n'
        ret += "left="+str(-1)+'\n'
    else:
        top = box[0];
        right = box[1];
        bottom = box[2];
        left = box[3];
        centerx = (right+left)/2
        centery = (top+bottom)/2

        boxWidth = right-left
        boxHeight = bottom-top

        ret += "width="+str(boxWidth/width)+'\n'
        ret += "height="+str(boxHeight/height)+'\n'
        ret += "centerx="+str(centerx/width)+'\n'
        ret += "centery="+str(centery/height)+'\n'
        ret += "top="+str(top)+'\n'
        ret += "right="+str(right)+'\n'
        ret += "bottom="+str(bottom)+'\n'
        ret += "left="+str(left)+'\n'
    return ret

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ('localhost', 10001)
sock.bind(server_address)
sock.listen(1)
while True:
    connection, client_address = sock.accept();
    try:
        while True:
            data = connection.recv(1024)
            if not data:
                break;
            if(data.decode() == "QUIT"):
                KILL = True
                break;
            result = locate(data.decode().strip())
            for face in result:
                print(face)
                connection.sendall(facialData(face).encode())
            break;
    finally:
        connection.close()
        if(KILL):
            break;
    #result = locate(pic)
    #for face in result:
    #print(face)
