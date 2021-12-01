#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Aug 11 14:19:08 2021

@author: zz
"""

import socket
import types
import time
import cv2
import pickle
import sys

#Host = socket.gethostbyname(socket.gethostname())
Host = "node2"
Port = 1234
Format = 'utf-8'
Header = 64
Buffer = 4096
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect((Host, Port))


def send_image():
    filename = sys.argv[1]
    # filename = 'image.jpg'

    file = open(filename, 'rb')
    file_data = file.read(Buffer)
    #print('sending data')
    while file_data:
        client.send(file_data)
        file_data = file.read(Buffer)
    #print('data sent')
    file.close()
    client.send(b'done')
    #print('getting prediction')

    pred_len = client.recv(Header).decode(Format)
    pred_len = int(pred_len)
    pred = b""
    while True:
        packet = client.recv(pred_len)
        if not packet:
            break
        pred+= packet
    pred = pickle.loads(pred)
    # print(pred)
    return pred


def get_defo(pred):
    base = len(pred)
    count = 0
    for i in range(base):
        if pred[i] < 0.5:
            count += 1
    defo = round(count*100/base, 2)
    print("Defoliation="+str(defo))
    return defo



def send_images():
    filename = 'DJI_0049.JPG'
    size = 108
    img = cv2.imread(filename)
    h, w, c = img.shape
    #print(h, w, c)
    hi = h // size
    wj = w // size
    count = hi * wj
    send_count = str(count).encode(Format)
    send_count += b' ' * (Header - len(send_count))
    client.send(send_count)
    for i in range(hi):
        for j in range(wj):
            cropped = img[i*size:(i+1)*size,j*size:(j+1)*size]
            cropped_bytes = len(cropped.tobytes())
            cropped_len = str(cropped_bytes).encode(Format)
            cropped_len += b' ' * (Header - len(cropped_len))
            client.send(cropped_len)

t0 = time.time()
pred = send_image()
defo = get_defo(pred)
#print(defo)
t1 = time.time() - t0
#print(t1)
# get_acc()
# disconnect()

# input()
# disconnect()
