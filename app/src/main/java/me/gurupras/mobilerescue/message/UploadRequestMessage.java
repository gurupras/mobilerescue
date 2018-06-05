package me.gurupras.mobilerescue.message;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import android.util.Log;
import static me.gurupras.androidcommons.Helper.pad;

import me.gurupras.androidcommons.Helper;
import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.message.Message;

public class UploadRequestMessage extends Message {
	private static final String TAG = Helper.createTag(Helper.MAIN_TAG, "UploadRequestMessage");
	private FileEntry entry;
	private String path;
	private long fileSize;
	private String checksum;
	private int isFile;

	static {
		Message.register(UploadRequestMessage.class);
	}

	public UploadRequestMessage() {
		super(UploadRequestMessage.class);
	}

	public UploadRequestMessage(FileEntry entry) {
		this();
		this.setEntry(entry);
		this.setPath(entry.getPath());
		this.setFileSize(entry.getSize());
		this.isFile = entry.isFile() == true ? 1 : 0;
	}

	@Override
	public void init() {
		setMessageLength((
				(Integer.SIZE * 2) / 8) /* message length + message type*/ +
				(Integer.SIZE / 8) /* path length */ +
				getEntry().getPath().length() /* path */ +
				(Integer.SIZE / 8) /* isFile */ +
				(Long.SIZE / 8) /* file size */ +
				64 /* sha-256 */
		);
	}

	@Override
	public byte[] build() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(getMessageLength());	/* message length */
		buffer.putInt(getMessageType());	/* message type */

		try {
			String path = getEntry().getPath();
			buffer.putInt(path.length());	/* path length */
			buffer.put(path.getBytes("UTF-8"));	/* path */
			buffer.putInt(isFile);	/* isFile */
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
			e.printStackTrace();
		}
		buffer.putLong(getEntry().getSize());

		byte[] hash = null;
		if(isFile == 1) {
			hash = MessageHelper.getHash(this.entry);
		}
		else {
			hash = MessageHelper.getHash("0");
		}
		buffer.put(hash);
		return buffer.array();
	}

	@Override
	public void parse(byte[] bytes) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		int pathLength = buffer.getInt();
		byte[] pathBytes = new byte[pathLength];
		buffer.get(pathBytes);
		this.setPath(new String(pathBytes, "UTF-8"));

		this.setFileSize(buffer.getLong());

		if(buffer.hasRemaining()) {
			throw new Exception("Message format error! Bytes Remaining :" + buffer.remaining());
		}

		this.setEntry(null);
	}
	
	public void parse(InputStream iStream) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		int messageLength = buffer.getInt();
		
		buffer = ByteBuffer.allocate(Integer.SIZE / 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		int messageType = buffer.getInt();
		
		buffer = ByteBuffer.allocate(Integer.SIZE / 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		int FilenameLength = buffer.getInt();
		
		buffer = ByteBuffer.allocate(FilenameLength);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		String filename = new String(buffer.array(), "UTF-8");
		
		buffer = ByteBuffer.allocate(Integer.SIZE / 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		int isFile = buffer.getInt();
		
		buffer = ByteBuffer.allocate(Long.SIZE / 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		long size = buffer.getLong();
		
		buffer = ByteBuffer.allocate(64);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		iStream.read(buffer.array());
		String checksum = new String(buffer.array(), "UTF-8");
		
		this.checksum = checksum;
		this.fileSize = size;
		this.isFile = isFile;
		this.path = filename;
	}

	/**
	 * @return isFile
	 */
	public int isFile() {
		return this.isFile;
	}
	
	/**
	 * @return the entry
	 */
	public FileEntry getEntry() {
		return entry;
	}

	/**
	 * @param entry the entry to set
	 */
	public void setEntry(FileEntry entry) {
		this.entry = entry;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the fileSize
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append(pad("entry", 30) + ":" + entry + "\n");
		builder.append(pad("path", 30)  + ":" + path + "\n");
		builder.append(pad("fileSize", 30)    + ":" + fileSize + "\n");
		builder.append(pad("checksum", 30)    + ":" + checksum + "\n");
		return builder.toString();
	}
}

