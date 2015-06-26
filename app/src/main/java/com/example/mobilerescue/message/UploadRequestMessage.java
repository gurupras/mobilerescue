package com.example.mobilerescue.message;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import android.util.Log;
import static com.example.androidcommons.Helper.pad;

import com.example.androidcommons.Helper;
import com.example.androidcommons.FileEntry;
import com.example.androidcommons.message.Message;

public class UploadRequestMessage extends Message {
	private static final String TAG = Helper.createTag(Helper.MAIN_TAG, "UploadRequestMessage");
	private FileEntry entry;
	private String path;
	private long fileSize;
	private String checksum;

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
	}

	@Override
	public void init() {
		setMessageLength(((Integer.SIZE * 2) / 8) + (Integer.SIZE / 8) + 
				getEntry().getPath().length() + (Long.SIZE / 8) + 64 /* sha-256 */);
	}

	@Override
	public byte[] build() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(getMessageLength());
		buffer.putInt(getMessageType());

		try {
			String path = getEntry().getPath();
			buffer.putInt(path.length());
			buffer.put(path.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
			e.printStackTrace();
		}
		buffer.putLong(getEntry().getSize());
		
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
			ByteBuffer fileBuffer = ByteBuffer.allocate(4096);
			InputStream instream  = new FileInputStream(entry.getFile());
			DigestInputStream dis = new DigestInputStream(instream, sha256);
			int readLength = 0;
			while((readLength = instream.read(fileBuffer.array())) != -1)
				sha256.update(fileBuffer.array(), 0, readLength);
			dis.close();
			checksum = Helper.toHex(sha256.digest());
			buffer.put(checksum.getBytes("UTF-8"));
		} catch(Exception e) {
			Log.e(TAG, "Checksum error for file '" + entry.getPath() + "'");
			Log.e(TAG, Log.getStackTraceString(e));
		}

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

