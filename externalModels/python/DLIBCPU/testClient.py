import socket
import sys
import time

start = time.time()
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

server_address = ('localhost', 10002)
sock.connect(server_address)

try:
    message = sys.argv[1]
    sock.sendto(message.encode(), server_address)
    while True:
        data = sock.recv(1024)
        if not data:
            break;
        print(data.decode())
        break;
finally:
    pass
    #print(time.time()-start)
