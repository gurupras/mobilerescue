package com.example.mobilerescue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import static com.example.mobilerescue.Helper.makeToast;

public class UploadService extends Thread implements Runnable {
	private static final String TAG = Helper.MAIN_TAG + "->UploadService";
	
	private FileEntry entry;
	private ProgressDialog dialog;
	
	public UploadService(FileEntry entry, ProgressDialog dialog) {
		this.entry = entry;
		this.dialog = dialog;
	}
	
	
	@Override
	public void run() {
//		Try to do the networking part
		String hostname = MainActivity.SettingsFragment.hostname;
		int port 		= 30000;
		Socket socket = null;
		
		try {
			final ProgressOutputStream outputStream = new ProgressOutputStream(null);
			
			final AtomicLong totalSize = new AtomicLong(0);
			final ArrayList<File> fileList = entry.getFiles();

			for(File f : fileList)
				totalSize.addAndGet(f.length());

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
			
			
			for(File file : fileList) {
				if(isCancelled.get())
					break;
				try {
					Log.d("Socket", "Attempting to connect to :" + hostname + "@" + port);
					socket = new Socket();
					socket.setSoTimeout(2000);
					socket.connect(new InetSocketAddress(hostname, port));
				} catch (Exception e) {
					Log.e("SocketException", "Message :" + e.getMessage());
					makeToast("Unable to connect to " + hostname);
					e.printStackTrace();
					return;
				}
				
				outputStream.setOutputStream(socket.getOutputStream());
				
				ByteBuffer buffer = ByteBuffer.allocate(4 + file.getAbsolutePath().length() + 8);
				
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putInt(file.getAbsolutePath().length());
				buffer.put(file.getAbsolutePath().getBytes());
				buffer.putLong(file.length());
				
				outputStream.write(buffer.array());
				
				long offset = (long) 0;
				InputStream instream = new FileInputStream(file);
				int defaultBufferSize = 1024 * 1024;
				while(offset < file.length()) {
					int bufferSize = (int) (defaultBufferSize > (file.length() - offset) ? (file.length() - offset) : defaultBufferSize);
					ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);
					instream.read(fileBuffer.array());
					outputStream.write(fileBuffer.array());
					activity.runOnUiThread(new Runnable() {
						public void run() {
							dialog.setProgress((int) ((outputStream.getBytesWritten() * 100) / totalSize.get()));
						}
					});
					offset += bufferSize;
				}
				instream.close();
				outputStream.close();
				Log.d(TAG, "Finished transferring file '" + file.getAbsolutePath() + "'");
				
			}
			dialog.dismiss();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
