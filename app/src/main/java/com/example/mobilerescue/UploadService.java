package com.example.mobilerescue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.example.androidcommons.AndroidApplication;
import com.example.androidcommons.FileEntry;
import com.example.androidcommons.Helper;
import com.example.androidcommons.ProgressOutputStream;
import com.example.androidcommons.message.Message;
import com.example.mobilerescue.message.UploadRequestMessage;
import com.example.mobilerescue.message.UploadResponseMessage;
import com.example.mobilerescue.message.UploadResponseMessage.UploadResponseType;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import static com.example.androidcommons.Helper.makeToast;

public class UploadService extends Thread implements Runnable {
	private static final String TAG = Helper.MAIN_TAG + "->Upload";

	private FileEntry entry;
	private ProgressDialog dialog;
	private AtomicLong totalSize, bytesWritten;

	public UploadService(FileEntry entry, ProgressDialog dialog) {
		this.entry = entry;
		this.dialog = dialog;
		this.totalSize = new AtomicLong(0);
		this.bytesWritten = new AtomicLong(0);
	}


	@Override
	public void run() {
//		Try to do the networking part
		String hostname = MainActivity.SettingsFragment.hostname;
		int port = MainActivity.SettingsFragment.port;
		Socket socket = null;

		try {
			final ProgressOutputStream outputStream = new ProgressOutputStream(null);

			final ArrayList<FileEntry> fileList = entry.getFiles();

			for (FileEntry e : fileList)
				totalSize.addAndGet(e.getFile().length());

			final Activity activity = AndroidApplication.getInstance().getCurrentActivity();
			final AtomicBoolean isCancelled = new AtomicBoolean(false);
			Runnable r = new Runnable() {
				public void run() {
					dialog.setMax(100);
					dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					dialog.setIndeterminate(false);
					dialog.setTitle("Transfer");
					dialog.setCancelable(false);
					dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							isCancelled.set(true);
						}
					});
					dialog.show();
				}
			};
			activity.runOnUiThread(r);


			for (FileEntry entry : fileList) {
				File file = entry.getFile();
				if (isCancelled.get())
					break;
				try {
					Log.d(TAG, "Attempting to connect to :" + hostname + "@" + port);
					socket = new Socket();
					socket.setSoTimeout(10000);
					socket.connect(new InetSocketAddress(hostname, port));
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
					makeToast("Unable to connect to " + hostname);
					e.printStackTrace();
					dialog.cancel();
					return;
				}

				outputStream.setOutputStream(socket.getOutputStream());

				UploadRequestMessage urqm = new UploadRequestMessage(entry);
				urqm.init();
				byte[] requestBytes = urqm.build();
				outputStream.write(requestBytes);
				outputStream.flush();

				// Wait for server to send info on whether this file is needed
				UploadResponseMessage ursm = new UploadResponseMessage();
				ursm.init();
				ByteBuffer responseBytes = ByteBuffer.allocate(ursm.getMessageLength());
				DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
				socketInputStream.read(responseBytes.array());
				ursm = (UploadResponseMessage) Message.parseMessage(responseBytes.array());

				if (ursm.getResponse() == UploadResponseType.FILE_FOUND) {
					socket.close();
					Log.d(TAG, "File Exists! '" + file.getAbsolutePath() + "'");
					bytesWritten.addAndGet(file.length());
					activity.runOnUiThread(dialogUpdateRunnable);
					continue;
				}

				long offset = (long) 0;
				InputStream instream = new FileInputStream(file);
				int defaultBufferSize = 1024 * 1024;
				while (offset < file.length()) {
					int bufferSize = (int) (defaultBufferSize > (file.length() - offset) ? (file.length() - offset) : defaultBufferSize);
					ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);
					instream.read(fileBuffer.array());
					outputStream.write(fileBuffer.array());
					bytesWritten.addAndGet(bufferSize);
					offset += bufferSize;
					activity.runOnUiThread(dialogUpdateRunnable);
				}
				instream.close();
				outputStream.close();
				Log.d(TAG, "Finished transferring file '" + file.getAbsolutePath() + "'");

			}
			dialog.dismiss();
		} catch (Exception e) {
			Log.e(TAG, "Failed to upload", e);
			e.printStackTrace();
		} finally {
			dialog.cancel();
		}
	}

	private Runnable dialogUpdateRunnable = new Runnable() {
		public void run() {
			dialog.setProgress((int) ((bytesWritten.get() * 100) / totalSize.get()));
		}
	};
}
