package com.example.androidcommons;

import android.util.Log;

import com.example.androidcommons.FileMetadata.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class FileEntry extends HashSet<FileEntry> {
	private static final long serialVersionUID = 1594311386293439370L;

	private static final String TAG = Helper.MAIN_TAG + "->FileEntry";
	
	private FileEntry parent;
	private FileMetadata metadata;

	public FileEntry(FileEntry parent, String path) {
		this.parent = parent;
		this.metadata = new FileMetadata(path);
	}
	
	public FileEntry getParent() {
		return this.parent;
	}
	
	public void addChildEntry(FileEntry entry) {
		this.add(entry);
	}
	
	public FileEntry get(String childName) {
		for(FileEntry child : this) {
			if(child.getName().equals(childName))
				return child;
		}
		return null;
	}
	
	public String getName() {
		return this.getFile().getName();
	}
	
	public String getPath() {
		StringBuilder sb = new StringBuilder();
		if(getParent() == null)
			sb.append(this.getFile().getAbsolutePath());
		else
			sb.append(getParent().getPath() + getName());
		if(this.getFileMetadata().getFileType() == FileType.DIRECTORY)
			sb.append("/");
		return sb.toString();
	}
	
	public FileMetadata getFileMetadata() {
		return this.metadata;
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder(this.getFile().getName() + "\n");
		if(size() != 0) {
			for(FileEntry childEntry : this) {
				String s = childEntry.toString();
				for(String line : s.toString().split("\n")) {
					sb.append("        " + line + "\n");
				}
			}
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		FileEntry entry = null;
		try {
			entry = (FileEntry) o;
			if(this.getName().equals(entry.getName()))
				return true;
		} catch (ClassCastException e) {
		}
		return false;
	}
	
	public File getFile() {
		return this.getFileMetadata().getFile();
	}
	
	
	public long getSize() {
		if(this.getFile().isDirectory()) {
			long totalSize = 0;
			this.buildTree();
			for(FileEntry child : this)
				totalSize += child.getSize();
			return totalSize;
		}
		else
			return this.getFile().length();
	}
	
	public ArrayList<FileEntry> getFiles() {
		this.buildTree();
		
		ArrayList<FileEntry> entryList = new ArrayList<FileEntry>();
		if(this.getFile().isDirectory()) {
			for(FileEntry entry : this) {
				if(entry.getFile().isDirectory()) {
					ArrayList<FileEntry> subList = entry.getFiles();
					entryList.addAll(subList);
				}
				else
					entryList.add(entry);
			}
		}
		else
			entryList.add(this);
		return entryList;
	}
	
	public static FileEntry buildTree(String path) {
		FileEntry root = new FileEntry(null, path);
		root.buildTree();
		return root;
	}
	
	public boolean buildTree() {
		TimeKeeper tk = new TimeKeeper();
		tk.start();
		boolean wasUpdated = false;
		
		File file = getFile();
		
		if(!file.isDirectory())
			return false;
		
		if(this.size() != file.listFiles().length) {
			this.clear();
//			ExecutorService executor = Executors.newFixedThreadPool(3);
			for(File f : file.listFiles()) {
				final FileEntry entry = new FileEntry(this, f.getAbsolutePath());
				this.add(entry);
				wasUpdated = true;
//				executor.execute(new Runnable() {
//					public void run() {
//						entry.buildTree();
//					}
//				});
			}
//			executor.shutdown();
//			try {
//				while(!executor.awaitTermination(100, TimeUnit.MILLISECONDS));
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		tk.stop();
		Log.d(TAG + "->buildTree", "Time taken :" + tk);
		return wasUpdated;
	}
}
