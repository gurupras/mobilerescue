package me.gurupras.mobilerescue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import me.gurupras.androidcommons.AndroidApplication;
import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.FileExplorerFragment;
import me.gurupras.androidcommons.FileMetadata.FileType;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import me.gurupras.androidcommons.FilesAdapter;
import me.gurupras.androidcommons.Helper;
import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.TimeKeeper;

public class RescueFileExplorerFragment extends FileExplorerFragment {
	private MainActivity activity;
	private boolean wasDialogShowingOnPause = false;

	public RescueFileExplorerFragment() {
		super();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.filesListView.setOnItemLongClickListener(filesListViewItemLongClickListener);
		if (wasDialogShowingOnPause) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.progressDialog.show();
				}
			});
		}
	}

	@Override
	public void onPause() {
		if (activity.progressDialog.isShowing()) {
			wasDialogShowingOnPause = true;
		}
		super.onPause();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.activity = (MainActivity) AndroidApplication.getInstance().getCurrentActivity();
		this.activity.progressDialog = new TransferProgressDialog(activity);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	private OnItemLongClickListener filesListViewItemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					List<String> operationsList = new LinkedList<String>();
//					Add only DOWNLOAD and DELETE
					operationsList.add("UPLOAD");
					operationsList.add("UPLOAD & DELETE");

					final String[] operations = new String[operationsList.size()];
					operationsList.toArray(operations);

					int itemPosition = position;

					final AtomicBoolean shouldDeleteAfterUpload = new AtomicBoolean(false);

					FilesAdapter adapter = ((FilesAdapter) filesListView.getAdapter());
					FileEntry adapterEntry = null;
					if (itemPosition == 0) {
						if (adapter == null)
							return;
						adapterEntry = adapter.getEntry();
					} else if (itemPosition == 1) {
						adapterEntry = adapter.getParentAdapter().getEntry();
					} else {
						// Position is non-zero..handle the '..' and '.' hack
						adapterEntry = adapter.getChildAdapterList().get(itemPosition).getEntry();
					}
					final FileEntry entry = adapterEntry;
					final String path = entry.getPath();

					AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
					dialog.setTitle(path);
					dialog.setItems(operations, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String option = operations[which];
							if (option.equals("UPLOAD & DELETE")) {
								shouldDeleteAfterUpload.set(true);
							}
							if (option.contains("UPLOAD")) {
								Log.d(TAG, "Trying to upload :" + path);
								UploadService upload = new UploadService(entry, activity.progressDialog, shouldDeleteAfterUpload.get());
								upload.start();
							} else {
								Log.e(TAG, "Unimplemented option  :" + option);
							}
						}
					});

					dialog.setNegativeButton("Cancel", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}

					});
					dialog.create();
					dialog.show();
				}
			});
			return true;
		}
	};
}
