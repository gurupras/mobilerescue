import socket
import sys, struct, os
import time
import argparse
import hashlib

from abc import abstractmethod

import multiprocessing
import threading
from multiprocessing.pool import ThreadPool

import tempfile

import logging
from pycommons import generic_logging
generic_logging.init(level=logging.DEBUG)
logger = logging.getLogger()

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
		header  = struct.pack('<i', self.message_length)
		header += struct.pack('<i', self.message_type)
		header += struct.pack('<i', self.response)
		return header

class UploadRequestMessage(Message):
	def __init__(self, filename, is_file, size, last_access, last_modified, checksum):
		self.filename = filename
		self.is_file = True if is_file == 1 else False
		self.size = size
		self.last_access = last_access
		self.last_modified = last_modified
		self.checksum = checksum

def decode_header(sock):
	message_length = struct.unpack('<i', sock.recv(4))[0]
	message_type   = struct.unpack('<i', sock.recv(4))[0]

	filename_len   = struct.unpack('<i', sock.recv(4))[0]
	filename       = struct.unpack('<%ds' % (filename_len), \
						sock.recv(filename_len))[0]
	is_file        = struct.unpack('<i', sock.recv(4))[0]
	size           = struct.unpack('<q', sock.recv(8))[0]
	last_access    = struct.unpack('<q', sock.recv(8))[0]
	last_modified  = struct.unpack('<q', sock.recv(8))[0]

	checksum       = struct.unpack('<64s', sock.recv(64))[0]

	return UploadRequestMessage(filename, is_file, size, last_access, last_modified, checksum)


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

	logger.info('Received file :%s\n' % (filename))

def compute_sha256(filename):
	sha256 = hashlib.sha256()
	with open(filename, 'rb') as f:
		for chunk in iter(lambda: f.read(2**20), b''):
			sha256.update(chunk)
	return sha256

def setup_parser():
	parser = argparse.ArgumentParser()
	parser.add_argument('-o', '--out', action="store", type=str, default=None, help='Path to store received files')
	parser.add_argument('-p', '--port', action="store", type=int, default=30000, help='Port to use for the server')
	parser.add_argument('-t', '--test', action="store", type=str, help='Test hash function')
	parser.add_argument('-T', '--threads', action="store", type=int, default=multiprocessing.cpu_count() * 2, help='Number of threads to use. Default = CPUS*2')
	parser.add_argument('-v', '--verbose', action="store_true", help='Enable verbose logging')
	return parser

def process(sock, args):
	urqm = decode_header(sock)
	filename = urqm.filename
	size = urqm.size
	checksum = urqm.checksum

	logger.info('Filename :' + filename)
	logger.info('Size     :' + str(size))

	if filename.startswith('/'):
		filename = os.path.join(args.out, filename[1:])
	else:
		filename = os.path.join(args.out, filename)

	logger.info('Out path :' + filename)

	if not urqm.is_file:
		try:
			os.makedirs(filename)
		except:
			assert os.path.exists(filename)
	else:
		dirname = os.path.dirname(filename)
		try:
			os.makedirs(dirname)
		except Exception:
			assert os.path.exists(dirname)

	urm = UploadResponseMessage()

	# If directory, we already created it. Just return with FILE_FOUND
	if not urqm.is_file:
		urm.response = UploadResponseMessage.FILE_FOUND
		message = urm.build()
		sock.send(message)
		return

	# It's a file..handle it
	if os.path.exists(filename):
		my_checksum = compute_sha256(filename).hexdigest()
		if my_checksum != checksum:
			logger.warn('File exists but checksums don\'t match!')
			logger.warn('  Request checksum   :%s' % (checksum))
			logger.warn('  My checksum        :%s' % (my_checksum))
			os.remove(filename)
			urm.response = UploadResponseMessage.FILE_NOT_FOUND
			message = urm.build()
			sock.send(message)
			recv_file(filename, size, sock)
		else:
			logger.info('File exists, checksums match!')
			# Update last modified if needed
			urm.response = UploadResponseMessage.FILE_FOUND
			message = urm.build()
			sock.send(message)
	else:
		urm.response = UploadResponseMessage.FILE_NOT_FOUND
		message = urm.build()
		sock.send(message)
		recv_file(filename, size, sock)
	# After doing anything with this file, update its times
	if urqm.last_access == 0:
		urqm.last_access = time.time() * 1000
	if urqm.last_modified == 0:
		urqm.last_modified = time.time() * 1000
	os.utime(filename, ns=(urqm.last_access * 1000000, urqm.last_modified * 1000000))

def main(argv):
	global args
	parser = setup_parser()
	args   = parser.parse_args(argv[1:])

	if args.test:
		print compute_sha256(args.test).hexdigest()
		return

	if not args.out:
		args.out = tempfile.mkdtemp(dir=os.getcwd())
	if not os.path.exists(args.out):
		logger.critical("Out path '%s' does not exist!" % (args.out))
		sys.exit(-1)

	logger.info('Port:          %d' % (args.port))
	logger.info('Output path:   %s' % (args.out))
	logger.info('# Threads:     %d' % (args.threads))

	# Initialize the pool
	pool = ThreadPool(processes=args.threads)

	host = ''
	port = args.port


	server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

	try:
		server_socket.bind((host, port))
	except socket.error as e:
		logger.critical('Bind failed! :' + e[1])
		sys.exit(-1)

	server_socket.listen(10)

	logger.info('Waiting for incoming connection ...')
	while 1:
		sock, addr = server_socket.accept()
		logger.info('Received connection from :%s' % (str(addr)))
#pool.apply_async(process, sock, callback=None)
		p = multiprocessing.Process(target=process, args=(sock, args))
		p.start()

if __name__ == "__main__":
	main(sys.argv)
