package com.example.mobilerescue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.example.androidcommons.AndroidApplication;
import com.example.androidcommons.FileEntry;
import com.example.androidcommons.Helper;
import com.example.mobilerescue.message.UploadRequestMessage;
import com.example.mobilerescue.message.UploadResponseMessage;
import com.example.mobilerescue.message.UploadResponseMessage.UploadResponseType;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import static com.example.androidcommons.Helper.makeToast;

public class DownloadService extends Thread implements Runnable {
	private static final String TAG = Helper.createTag(DownloadService.class);
	
	public int port;

	private ProgressDialog dialog;
	private AtomicLong totalSize, bytesWritten;
	private AtomicBoolean isCancelled;
	private Activity activity;
	private FileEntry root;
	
	public DownloadService() {
		this(AndroidApplication.externalPath.getAbsolutePath());
	}

	public DownloadService(String basePath) {
		this.activity = AndroidApplication.getInstance().getCurrentActivity();
		this.dialog = new ProgressDialog(AndroidApplication.getInstance().getCurrentActivity());
		this.root = new FileEntry(null, basePath);
		this.totalSize = new AtomicLong(0);
		this.bytesWritten = new AtomicLong(0);
		this.isCancelled = new AtomicBoolean(false);
		this.port = MainActivity.SettingsFragment.serverPort; 
		
		/* Build the tree */
		new Thread(new Runnable() {
			public void run() {
				root.buildTree();
				Log.d(TAG, "Finished building file entry tree");
			}
		}).run();
	}

	public void init() {
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			init();
			/* Start server and listen */
			serverSocket = new ServerSocket();
			serverSocket.setSoTimeout(1000);
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));
			makeToast("Server running on port: " + port);
			while(true) {
				if(Thread.interrupted()) {
					Log.d(TAG, "Interrupted ...");
					break;
				}
				Socket clientSocket = null;
				try {
					clientSocket = serverSocket.accept();
				} catch(SocketTimeoutException e) {
					/* Accept can timeout..that's fine */
					continue;
				}
				/* We now have a client. Handle it */
				handleClient(clientSocket);
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to download", e);
			e.printStackTrace();
			makeToast("Terminating download service");
		} finally {
			dialog.cancel();
			try {
				serverSocket.close();
			} catch(Exception e) {}
		}
		Log.d(TAG, "Exiting ...");
	}

	private void handleClient(Socket socket) throws Exception {
		UploadRequestMessage urqm = new UploadRequestMessage();
		urqm.parse(socket.getInputStream());
		
		UploadResponseMessage ursm = new UploadResponseMessage();
		ursm.init();

		boolean shouldReceive = false;
		
		if(urqm.isFile() == 0) {
			ursm.setResponse(UploadResponseType.FILE_FOUND);
		}
		else if(urqm.isFile() == 1) {
			String path = urqm.getPath();
			String subPath = null;
			if(path.contains(root.getPath())) {
				subPath = path.substring(path.lastIndexOf(root.getPath()) + root.getPath().length());
			}
			else if(urqm.getPath().startsWith("/")) {
				subPath = urqm.getPath().substring(1);
			}
			File absFile = new File(new File(root.getPath()), subPath);
			if(!absFile.exists()) {
				ursm.setResponse(UploadResponseType.FILE_NOT_FOUND);
				shouldReceive = true;
			}
		}
		else {
			throw new IllegalStateException("Unknown value for isFile(): " + urqm.isFile());
		}
		socket.getOutputStream().write(ursm.build());
		if(shouldReceive) {
			totalSize.set(urqm.getFileSize());
			activity.runOnUiThread(initDialog);
			receiveFile(urqm, socket);
		}
	}
	private void receiveFile(UploadRequestMessage urqm, Socket socket) throws Exception {
		// Try to do the networking part
		String path = urqm.getPath();
		File f = new File(path);
		File parent = f.getParentFile();
		try {
			if(!parent.exists()) {
				parent.mkdirs();
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to create directories for file: " + f.getAbsolutePath());
		}

		FileOutputStream outstream = new FileOutputStream(f);
		int defaultBufferSize = 1024 * 1024;
		long offset = 0;
		InputStream instream = socket.getInputStream();
		while (offset < urqm.getFileSize()) {
			int bufferSize = (int) (defaultBufferSize > (urqm.getFileSize() - offset) ?
					(urqm.getFileSize() - offset) : defaultBufferSize);
			ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);
			instream.read(fileBuffer.array());
			outstream.write(fileBuffer.array());
			bytesWritten.addAndGet(bufferSize);
			offset += bufferSize;
			activity.runOnUiThread(dialogUpdateRunnable);
		}
		dialog.dismiss();
		instream.close();
		outstream.close();
		Log.d(TAG, "Finished obtaining file '" + f.getAbsolutePath() + "'");
	}

	private Runnable dialogUpdateRunnable = new Runnable() {
		public void run() {
			dialog.setProgress((int) ((bytesWritten.get() * 100) / totalSize.get()));
		}
	};

	private Runnable initDialog = new Runnable() {
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
}
