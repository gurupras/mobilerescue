package me.gurupras.mobilerescue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import me.gurupras.androidcommons.AndroidApplication;
import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.Helper;
import me.gurupras.mobilerescue.message.UploadRequestMessage;
import me.gurupras.mobilerescue.wires.IWireEvents;
import me.gurupras.mobilerescue.wires.SocketWire;
import me.gurupras.mobilerescue.wires.Wire;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.util.Log;
import static me.gurupras.androidcommons.Helper.makeToast;

public class UploadService extends Thread implements Runnable {
	private static final String TAG = Helper.MAIN_TAG + "->Upload";

	private static final int POOL_SIZE = 4;

	private boolean shouldDeleteAfterUpload;
	private FileEntry entry;
	private TransferProgressDialog dialog;
	private AtomicLong totalSize, bytesWritten;
	private AtomicBoolean isCancelled;
	private ArrayList<FileEntry> fileList;
	private Activity activity;
	private ScheduledExecutorService executor;

	public UploadService(FileEntry entry, TransferProgressDialog dialog, boolean shouldDeleteAfterUpload) {
		this.shouldDeleteAfterUpload = shouldDeleteAfterUpload;
		this.entry = entry;
		this.dialog = dialog;
		this.totalSize = new AtomicLong(0);
		this.bytesWritten = new AtomicLong(0);
		this.isCancelled = new AtomicBoolean(false);
	}

	public UploadService(FileEntry entry, TransferProgressDialog dialog) {
		this(entry, dialog, false);
	}

	public void init() {
		fileList = entry.getFiles();
		for (FileEntry e : fileList)
			totalSize.addAndGet(e.getFile().length());

		activity = AndroidApplication.getInstance().getCurrentActivity();
		activity.runOnUiThread(initDialog);
		executor = Executors.newScheduledThreadPool(POOL_SIZE);
	}

	private Wire initializeWire() throws Exception {
		String hostname = MainActivity.SettingsFragment.hostname;
		int port = MainActivity.SettingsFragment.port;
		IWireEvents listener = new IWireEvents() {
			@Override
			public void onRemoteFileExists(UploadRequestMessage urqm) {
				FileEntry entry = urqm.getEntry();
				if(entry.isFile()) {
					Log.d(TAG, "File Exists! '" + entry.getPath() + "'");
				}
				bytesWritten.addAndGet(entry.getSize());
				activity.runOnUiThread(dialogUpdateRunnable);
			}

			@Override
			public void onWireProgress(int transferred) {
				bytesWritten.addAndGet(transferred);
				activity.runOnUiThread(dialogUpdateRunnable);
			}
		};
		Wire wire = new SocketWire();
		wire.addListener(listener);
		wire.connect(hostname, port);
		return wire;
	}

	private HashMap<Long, Wire> wireMap = new HashMap<Long, Wire>();

	private Wire getWireForThread(long threadId) throws Exception {
		if (wireMap.get(threadId) == null) {
			wireMap.put(threadId, initializeWire());
		}
		return wireMap.get(threadId);
	}

	@Override
	public void run() {
		final AtomicInteger failedCount = new AtomicInteger(0);

		init();

		PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "upload wakelock");
		wakeLock.acquire();

		try {
			for (FileEntry entry : fileList) {
				if (isCancelled.get())
					break;
				try {
					executor.execute(new SendFileRunnable(entry));
//					new SendFileRunnable(entry).run();
				} catch(Exception e) {
					Log.e(TAG, "Failed to upload", e);
					makeToast("Failed " + entry.getPath() + ": " + e.getMessage());
					e.printStackTrace();
					failedCount.incrementAndGet();
				}
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			dialog.dismiss();
		} catch (Exception e) {
			Log.e(TAG, "Failed to upload", e);
			makeToast("Failed!: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(failedCount.get() > 0) {
				makeToast("Failed: " + failedCount);
			}
			try {
				for (Wire wire : wireMap.values()) {
					wire.disconnect();
				}
			} catch (Exception e1) {
				Log.e(TAG, "Failed to close connection: " + e1.getMessage(), e1);
			}
			dialog.cancel();
			wakeLock.release();
		}
	}

	private class SendFileRunnable implements Runnable {
		private FileEntry entry;
		private Wire wire;

		public SendFileRunnable(FileEntry entry) {
			this.entry = entry;
		}
		
		public void run() {
			if (isCancelled.get()) {
				return;
			}
//			Get wire
			long threadId = Thread.currentThread().getId();
			try {
				wire = getWireForThread(threadId);
				sendFile(entry);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		private void sendFile(final FileEntry entry) throws Exception {
			// Try to do the networking part
			activity.runOnUiThread(new Runnable() {
				public void run() {
					dialog.setTitle("Transfer: " + entry.getName());
				}
			});

			wire.upload(entry);
			if(shouldDeleteAfterUpload) {
				entry.getFile().delete();
			}
		}
	}

	private Runnable dialogUpdateRunnable = new Runnable() {
		public void run() {
			dialog.setProgress(bytesWritten.get());
		}
	};

	private Runnable initDialog = new Runnable() {
		public void run() {
			dialog.setMax(totalSize.longValue());
			dialog.setProgress(0);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setTitle(entry.getName());
			dialog.setCancelable(false);
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					isCancelled.set(true);
				}
			});
			dialog.show();
		}
	};


	public void setShouldDeleteAfterUpload(boolean shouldDeleteAfterUpload) {
		this.shouldDeleteAfterUpload = shouldDeleteAfterUpload;
	}
}
