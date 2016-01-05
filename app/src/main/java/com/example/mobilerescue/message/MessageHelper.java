package com.example.mobilerescue.message;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.example.androidcommons.FileEntry;
import com.example.androidcommons.Helper;

import android.util.Log;

public class MessageHelper {
	private static final String TAG = Helper.createTag(MessageHelper.class);
	
	public static byte[] getHash(FileEntry entry) {
		String checksum = null;
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
			ByteBuffer fileBuffer = ByteBuffer.allocate(4096);
			InputStream instream = new FileInputStream(entry.getFile());
			DigestInputStream dis = new DigestInputStream(instream, sha256);
			int readLength = 0;
			/* XXX: Replace this with call to getHash(string); */
			while ((readLength = instream.read(fileBuffer.array())) != -1)
				sha256.update(fileBuffer.array(), 0, readLength);
			dis.close();
			checksum = Helper.toHex(sha256.digest());
			return checksum.getBytes("UTF-8");
		} catch (Exception e) {
			Log.e(TAG, "Checksum error for file '" + entry.getPath() + "'");
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return null;
	}

	public static byte[] getHash(String string) {
		String checksum = null;
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
			byte[] bytes = string.getBytes("UTF-8");
			sha256.update(bytes);
			checksum = Helper.toHex(sha256.digest());
			return checksum.getBytes("UTF-8");
		} catch (Exception e) {
			Log.e(TAG, "Checksum error for string '" + string + "'");
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return null;
	}
}
