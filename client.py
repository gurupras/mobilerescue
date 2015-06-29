import socket
import struct
import os
import sys

host = '127.0.0.1'
port = 30000

if len(sys.argv) != 2:
    print __file__ + ' <path>'
    sys.exit(-1)

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((host, port))

if not os.path.exists(sys.argv[1]):
    print sys.argv[1] + ' - Does not exist!'
    sys.exit(-1)

input_file = open(sys.argv[1])
full_path = os.path.abspath(input_file.name)
filename_len = len(full_path)
file_size = os.stat(full_path).st_size

header = struct.pack('i', filename_len)
header += struct.pack(str(filename_len) + 's', full_path)
header += struct.pack('q', file_size)

client_socket.send(header)

offset = 0
default_buf_size = 1024 * 1024

while offset < file_size:
    buf_size = (file_size - offset) if \
            default_buf_size > (file_size - offset) else default_buf_size
    data = input_file.read(buf_size)
    client_socket.send(data)
    offset += len(data)

client_socket.close()
input_file.close()

