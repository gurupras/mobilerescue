package com.example.mobilerescue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.example.androidcommons.AndroidApplication;
import com.example.androidcommons.FileEntry;
import com.example.androidcommons.FileExplorerFragment;
import com.example.androidcommons.FileExplorerFragment.FilesAdapter;
import com.example.androidcommons.FileMetadata.FileType;

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

import com.example.androidcommons.Helper;
import com.example.androidcommons.FileEntry;
import com.example.androidcommons.TimeKeeper;

public class RescueFileExplorerFragment extends FileExplorerFragment {
	private MainActivity activity;

	public RescueFileExplorerFragment() {
		super();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.filesListView.setOnItemLongClickListener(filesListViewItemLongClickListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.activity = (MainActivity) AndroidApplication.getInstance().getCurrentActivity();
		this.activity.progressDialog = new ProgressDialog(activity);
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

					final String[] operations = new String[operationsList.size()];
					operationsList.toArray(operations);

					int itemPosition = position;

					FilesAdapter adapter = ((FilesAdapter) filesListView.getAdapter());
					FileEntry adapterEntry = null;
					if (itemPosition == 0) {
						adapter = adapter.getParentAdapter();
						if (adapter == null)
							return;
						adapterEntry = adapter.getEntry();
					} else if (itemPosition == 1) {
						adapterEntry = adapter.getEntry();
					} else {
						// Position is non-zero..handle the '..' and '.' hack
						itemPosition -= 2;
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
							if (option.equals("UPLOAD")) {
								Log.d(TAG, "Trying to upload :" + path);
								UploadService upload = new UploadService(entry, activity.progressDialog);
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
