package com.example.androidcommons;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
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

import com.example.androidcommons.FileMetadata.FileType;

import java.util.LinkedList;
import java.util.List;

public class FileExplorerFragment extends Fragment {
	protected static final String TAG = Helper.MAIN_TAG + "->FileExplorerFragment";
	
	protected static Boolean isInitComplete = false;
	
	protected View rootView;
	protected ListView filesListView;
	protected FilesAdapter rootAdapter;
	protected Activity activity;
	
	public FileExplorerFragment() {
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_fileexplorer, container, false);
		
		filesListView  = (ListView) rootView.findViewById(R.id.files_listView);
		this.activity = getActivity();

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		FileEntry root = FileEntry.buildTree(AndroidApplication.externalPath.getAbsolutePath());
		SharedPreferences settings = activity.getSharedPreferences("fileExplorer", Activity.MODE_PRIVATE);
		SharedPreferences.Editor edit = settings.edit();
		edit.putString("root", root.getPath());
		edit.commit();
		this.rootAdapter = FilesAdapter.buildListAdapter(root);
		filesListView.setAdapter(rootAdapter);
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
//						Special case '..'
//						We need to go back to parent
						if(adapter.getParentAdapter() != null) {
							newAdapter = adapter.getParentAdapter();
						}
					}
					else if(position == 1) {
						// Special case '.'
						newAdapter = adapter;
					}
					else {
//						Since position 0 is occupied for '..', we subtract 1 from position to get child
						int newPosition = position - 2;
						FilesAdapter childAdapter = adapter.getChildAdapterList().get(newPosition);
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
				}
			});
		}
	};

	protected static class FilesAdapter extends ArrayAdapter<String> {
		private static final String TAG = FileExplorerFragment.TAG + "->FilesAdapter";
		
		private List<FilesAdapter> childAdapterList; 
		private FileEntry entry;
		private FilesAdapter parentAdapter;
		
		private FilesAdapter(FileEntry entry, FilesAdapter parent) {
			super(AndroidApplication.getInstance(), android.R.layout.simple_list_item_1);
			this.parentAdapter = parent;
			this.entry = entry;
			this.childAdapterList = new LinkedList<FilesAdapter>();
		}
		
		
		public List<FilesAdapter> getChildAdapterList() {
			return childAdapterList;
		}

		public FileEntry getEntry() {
			return entry;
		}

		public FilesAdapter getParentAdapter() {
			return parentAdapter;
		}

		public static FilesAdapter buildListAdapter(FileEntry root) {
			return buildListAdapter(root, null);
		}
		
		private static FilesAdapter buildListAdapter(FileEntry entry, FilesAdapter parent) {
			FilesAdapter adapter = new FilesAdapter(entry, parent);
			adapter.build();
			return adapter;
		}
		
		private void build() {
			TimeKeeper tk = new TimeKeeper();
			if(this.getEntry().buildTree() || this.getCount() != this.getEntry().size()) {
				tk.start();
				super.clear();
				this.add("..");
				this.add(".");
				for(FileEntry child : entry) {
					FilesAdapter childAdapter = new FilesAdapter(child, this);
					this.childAdapterList.add(childAdapter);
					this.add(child.getName());
				}
			}
			else
				tk.start();
			if(this.getCount() == 0)
				this.add("..");	//XXX: Hack for empty directories
			
			tk.stop();
			Log.d(TAG + "->build", "Time Taken :" + tk);
		}
	}
}
