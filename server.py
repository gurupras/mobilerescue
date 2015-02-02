import socket
import sys
import struct
import os
import hashlib


class Message(object):
	@staticmethod
	def send_file_not_found(sock):
		header = struct.pack('i', 12)
		header += struct.pack('i', 1)	# 1 = UploadResponseMessage
		header += struct.pack('i', 0)	# 0 = FileNotFound
		sock.send(header)
	@staticmethod
	def send_file_found(sock):
		header = struct.pack('i', 12)
		header += struct.pack('i', 1)	# 1 = UploadResponseMessage
		header += struct.pack('i', 0)	# 1 = FileFound
		sock.send(header)

def decode_header(sock):
	message_length = struct.unpack('i', sock.recv(4))[0]
	message_type   = struct.unpack('i', sock.recv(4))[0]

	filename_len   = struct.unpack('i', sock.recv(4))[0]
	filename       = struct.unpack('%ds' % (filename_len), \
						sock.recv(filename_len))[0]
	size           = struct.unpack('q', sock.recv(8))[0]

	checksum       = struct.unpack('64s', sock.recv(64))[0]

	return filename, size, checksum


def recv_file(filename, size, sock):
	fd = open(filename, 'wb')
	offset = 0
	max_buf_size = 2 * 1024 * 1024
	while offset < size:
		buf_size = max_buf_size if (size - offset) > max_buf_size else (size - offset)
		data = sock.recv(buf_size)
		fd.write(data)
		offset += len(data)
	fd.close()
	print 'Received file :%s' % (filename)
	print ''

def compute_sha256(filename):
	sha256 = hashlib.sha256()
	with open(filename, 'rb') as f:
		sha256.update(f.read())
	return sha256

def main(argv):
	if len(argv) != 2:
		print __file__ + ' <out_path>'
		sys.exit(-1)

	if not os.path.exists(argv[1]):
		print 'Out path does not exist!'
		sys.exit(-1)


	host = ''
	port = 30000
	out_path = argv[1]


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
		filename, size, checksum = decode_header(sock)
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
			my_checksum = compute_sha256(filename).hexdigest()
			if my_checksum != checksum:
				print 'File exists but checksums don\'t match!'
				print '  Request checksum   :%s' % (checksum)
				print '  My checksum        :%s' % (my_checksum)
				os.remove(filename)
				Message.send_file_not_found(sock)
				recv_file(filename, size, sock)
			else:
				print 'File exists, checksums match!'
				Message.send_file_found(sock)
		else:
			Message.send_file_not_found(sock)
			recv_file(filename, size, sock)

if __name__ == "__main__":
	main(sys.argv)
