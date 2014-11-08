import socket
import sys
import struct
import os


def decode_header(sock):
    filename_len = struct.unpack('i', sock.recv(4))[0]
    filename = struct.unpack(str(filename_len) + 's', \
            sock.recv(filename_len))[0]
    size = struct.unpack('q', sock.recv(8))[0]

    return filename, size


if len(sys.argv) != 2:
    print __file__ + ' <out_path>'
    sys.exit(-1)

if not os.path.exists(sys.argv[1]):
    print 'Out path does not exist!'
    sys.exit(-1)


host = ''
port = 30000
out_path = sys.argv[1]


server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

try:
    server_socket.bind((host, port))
except socket.error as e:
    print 'Bind failed! :' + e[1]
    sys.exit(-1)

server_socket.listen(10)

while 1:
    sock, addr = server_socket.accept()
    print 'Received connection!'
    filename, size = decode_header(sock)
    print 'Filename :' + filename
    print 'Size     :' + str(size)

    if filename.startswith('/'):
        filename = out_path + '/' + filename[1:]

    print 'Out path :' + filename
    try:
        os.makedirs(os.path.dirname(filename))
    except Exception:
	    pass
    if os.path.exists(filename):
        os.remove(filename)
    fd = open(filename, 'wb')
    offset = 0
    max_buf_size = 2 * 1024 * 1024
    while offset < size:
        buf_size = max_buf_size if (size - offset) > max_buf_size else (size - offset)
        data = sock.recv(buf_size)
        fd.write(data)
        offset += len(data)
    fd.close()

