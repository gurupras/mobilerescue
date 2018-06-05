package me.gurupras.androidcommons;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Build;
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
import android.app.Activity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import me.gurupras.androidcommons.FileMetadata.FileType;

import java.util.LinkedList;
import java.util.List;

public class FileExplorerFragment extends Fragment {
	protected static final String TAG = Helper.MAIN_TAG + "->FileExplorerFragment";
	
	protected static Boolean isInitComplete = false;
	
	protected View rootView;
	protected ListView filesListView;
	protected FilesAdapter rootAdapter;

	public FilesAdapter getCurrentRoot() {
		return currentRoot;
	}

	protected FilesAdapter currentRoot;
	protected Activity activity;
	
	public FileExplorerFragment() {
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_fileexplorer, container, false);

		filesListView  = (ListView) rootView.findViewById(R.id.files_listView);
		this.activity = getActivity();

		Dexter.withActivity(activity)
				.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				.withListener(new MultiplePermissionsListener() {
					@Override
					public void onPermissionsChecked(MultiplePermissionsReport report) {
						Log.d(TAG, "Checked permissions: " + report);
					}

					@Override
					public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
						token.continuePermissionRequest();
					}
				})
				.check();

		return rootView;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void showPermissionRationale(PermissionRequest permission, final PermissionToken token) {
		String title = "Permission Rationale", message = "Please give us this permission!";
		switch(permission.getName()) {
			case Manifest.permission.READ_EXTERNAL_STORAGE:
			case Manifest.permission.WRITE_EXTERNAL_STORAGE:
				title = "Requesting EXTERNAL_STORAGE permissions";
				message = "We require these permissions to be able to read/write files from external storage";
				break;
		}
		new AlertDialog.Builder(activity).setTitle(title)
				.setMessage(message)
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						token.cancelPermissionRequest();
					}
				})
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						token.continuePermissionRequest();
					}
				})
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override public void onDismiss(DialogInterface dialog) {
						token.cancelPermissionRequest();
					}
				})
				.show();
	}

	public void showPermissionGranted(String permission) {
		Log.d(TAG, "Permission granted: " + permission);
		Helper.makeToast("Permission granted: " + permission);
	}

	public void showPermissionDenied(String permission, boolean isPermanentlyDenied) {
		Log.d(TAG, "Permission denied: " + permission);
		Helper.makeToast("Permission denied: " + permission);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (currentRoot == null) {
			FileEntry root = FileEntry.buildTree(AndroidApplication.externalPath.getAbsolutePath());
			SharedPreferences settings = activity.getSharedPreferences("fileExplorer", Activity.MODE_PRIVATE);
			SharedPreferences.Editor edit = settings.edit();
			edit.putString("root", root.getPath());
			edit.commit();
			this.rootAdapter = FilesAdapter.buildListAdapter(root);
			this.currentRoot = rootAdapter;
		}
		filesListView.setAdapter(currentRoot);
		filesListView.setOnItemClickListener(filesListViewItemListener);
	}
	
	private OnItemClickListener filesListViewItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					FilesAdapter adapter = (FilesAdapter) filesListView.getAdapter();
					FilesAdapter newAdapter = adapter;
					if(position == 0) {
						// Special case '.'
						newAdapter = adapter;
					}
					else if(position == 1) {
//						Special case '..'
//						We need to go back to parent
						if(adapter.getParentAdapter() != null) {
							newAdapter = adapter.getParentAdapter();
						}
					}

					else {
						FilesAdapter childAdapter = adapter.getChildAdapterList().get(position);
						if(childAdapter.getEntry().getFileMetadata().getFileType() == FileType.FILE) {
		//					This is a file..we don't support short clicks on files..only long clicks
						}
						else {
							newAdapter = childAdapter;
						}
					}
					
//					Call build to update
					newAdapter.build();
					final TextView pathTextView = (TextView) rootView.findViewById(R.id.path_textView);
					if(newAdapter != adapter)
						Log.d(TAG, "Navigated to  :" + newAdapter.getEntry().getPath());
					pathTextView.setText(newAdapter.getEntry().getPath());
					filesListView.setAdapter(newAdapter);
					currentRoot = newAdapter;
				}
			});
		}
	};

	public void refreshListView() {
		filesListView.setAdapter(currentRoot);
	}
}
