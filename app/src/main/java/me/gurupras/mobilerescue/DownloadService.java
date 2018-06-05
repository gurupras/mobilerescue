package me.gurupras.mobilerescue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import me.gurupras.androidcommons.AndroidApplication;
import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.Helper;
import me.gurupras.mobilerescue.message.UploadRequestMessage;
import me.gurupras.mobilerescue.message.UploadResponseMessage;
import me.gurupras.mobilerescue.message.UploadResponseMessage.UploadResponseType;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import static me.gurupras.androidcommons.Helper.makeToast;

public class DownloadService extends Thread implements Runnable {
	private static final String TAG = Helper.createTag(DownloadService.class);
	private FileEntry root;
	public int port;
	private ProgressDialog dialog;
	private Activity activity;

	private AtomicInteger numActiveDownloads = new AtomicInteger(0);

	public DownloadService() {
		this(AndroidApplication.externalPath.getAbsolutePath());
		this.activity = AndroidApplication.getInstance().getCurrentActivity();

		final AtomicBoolean dialogInitialized = new AtomicBoolean(false);
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Log.v(TAG, "Attempting to initialize dialog");
				dialog = new ProgressDialog(AndroidApplication.getInstance().getCurrentActivity());
				synchronized(DownloadService.this) {
					dialogInitialized.set(true);
					DownloadService.this.notifyAll();
				}
				Log.v(TAG, "Dialog initialized");
			}
		});

		synchronized(this) {
			if(dialogInitialized.get()) {
				// already initialized
				Log.v(TAG, "Dialog was already initialized. No need to wait");
			} else {
				try {
					Log.v(TAG, "Waiting for dialog to be initialized");
					this.wait();
					Log.v(TAG, "Finished waiting. Dialog initialized");
				} catch (Exception e) {
					Log.e(TAG, "Exception while waiting for notification: ", e);
				}
			}
		}
	}

	public DownloadService(String basePath) {
		this.root = new FileEntry(null, basePath);
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
			serverSocket.setSoTimeout(10000);
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
				final Socket cSocket = clientSocket;
				new Thread(new Runnable() {
					public void run() {
						try {
							handleClient(cSocket);
						} catch(Exception e) {
							Log.e(TAG, "Failed to download", e);
							e.printStackTrace();
							makeToast("Terminating download service");
						}
					}
				}).start();
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to download", e);
			e.printStackTrace();
			makeToast("Terminating download service");
		} finally {
			try {
				serverSocket.close();
			} catch(Exception e) {}
		}
		Log.d(TAG, "Exiting ...");
	}

	private void handleClient(Socket socket) throws Exception {
		Log.d(TAG, "Handing new client connection...");
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
			DownloadJob job = new DownloadJob(urqm, socket);
			job.receiveFile();
		}
	}

	private Runnable initDialog = new Runnable() {
		public void run() {
			dialog.setMax(100);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setTitle("Transfer");
			dialog.setCancelable(false);

			/*
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					isCancelled.set(true);
				}
			});
			*/
		}
	};

	private void showDialog() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				dialog.show();
			}
		});
	}

	private class DownloadJob {
		private final String TAG = Helper.createTag(DownloadJob.class);

		private AtomicLong totalSize, bytesWritten;
		private AtomicBoolean isCancelled;
		private Activity activity;
		private UploadRequestMessage urqm;
		private Socket socket;

		DownloadJob(UploadRequestMessage urqm, Socket socket) {
			this.urqm = urqm;
			this.socket = socket;
			this.activity = AndroidApplication.getInstance().getCurrentActivity();

			this.totalSize = new AtomicLong(urqm.getFileSize());
			this.bytesWritten = new AtomicLong(0);
			this.isCancelled = new AtomicBoolean(false);
		}

		private Runnable dialogUpdateRunnable = new Runnable() {
			public void run() {
				dialog.setTitle("Transfer: " + urqm.getPath());
				dialog.setProgress((int) ((bytesWritten.get() * 100) / totalSize.get()));
			}
		};

		private void receiveFile() throws Exception {
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

			activity.runOnUiThread(initDialog);

			FileOutputStream outstream = new FileOutputStream(f);
			int defaultBufferSize = 256 * 1024;
			long offset = 0;
			InputStream instream = socket.getInputStream();

			int numDownloads;
			synchronized (numActiveDownloads) {
				if (numActiveDownloads.incrementAndGet() == 1) {
					// Show dialog
					showDialog();
				}
			}

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

			synchronized (numActiveDownloads) {
				if (numActiveDownloads.decrementAndGet() == 0) {
					dialog.dismiss();
				}
			}

			instream.close();
			outstream.close();
			Log.d(TAG, "Finished obtaining file '" + f.getAbsolutePath() + "'");
		}
	}
}
