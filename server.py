import socket
import sys, struct, os
import argparse
import hashlib

from abc import abstractmethod


class Message(object):
	message_length = None
	message_type   = None

	@abstractmethod
	def build(self):
		raise Exception('Unimplemented')

class UploadResponseMessage(Message):
	response       = -1
	FILE_FOUND     = 1
	FILE_NOT_FOUND = 0

	def __init__(self):
		self.message_length = 4 + 4 + 4	# length + type + response
		self.message_type = 2

	def build(self):
		header  = struct.pack('i', self.message_length)
		header += struct.pack('i', self.message_type)
		header += struct.pack('i', self.response)
		return header

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
		for chunk in iter(lambda: f.read(2**20), b''):
			sha256.update(chunk)
	return sha256

def setup_parser():
	parser = argparse.ArgumentParser()
	parser.add_argument('-o', '--out', action="store", type=str, required=True, help='Path to store received files')
	parser.add_argument('-p', '--port', action="store", type=int, default=30000, help='Port to use for the server')
	parser.add_argument('-t', '--test', action="store", type=str, help='Test hash function')
	return parser

def main(argv):
	parser = setup_parser()
	args   = parser.parse_args(argv[1:])

	if not os.path.exists(args.out):
		print 'Out path does not exist!'
		sys.exit(-1)

	if args.test:
		print compute_sha256(args.test).hexdigest()
		return

	host = ''
	port = args.port
	out_path = args.out


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

		urm = UploadResponseMessage()
		if os.path.exists(filename):
			my_checksum = compute_sha256(filename).hexdigest()
			if my_checksum != checksum:
				print 'File exists but checksums don\'t match!'
				print '  Request checksum   :%s' % (checksum)
				print '  My checksum        :%s' % (my_checksum)
				os.remove(filename)
				urm.response = UploadResponseMessage.FILE_NOT_FOUND
				message = urm.build()
				sock.send(message)
				recv_file(filename, size, sock)
			else:
				print 'File exists, checksums match!'
				urm.response = UploadResponseMessage.FILE_FOUND
				message = urm.build()
				sock.send(message)
		else:
			urm.response = UploadResponseMessage.FILE_NOT_FOUND
			message = urm.build()
			sock.send(message)
			recv_file(filename, size, sock)

if __name__ == "__main__":
	main(sys.argv)
