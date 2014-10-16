package com.example.mobilerescue;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class FileMetadata implements Serializable {
	private static final long serialVersionUID = 9062350708385725981L;
	
	private static final String TAG = Helper.MAIN_TAG + "->FileMetadata";
	
	private File file;
	
	protected FileMetadata() {
	}
	
	public FileMetadata(File file) {
		this.file = file;
	}
	
	public FileMetadata(String path) {
		this.file = new File(path);
		if(!file.exists())
			throw new IllegalStateException("File does not exist! :" + file.getAbsolutePath());
	}
	
	public synchronized FileType getFileType() {
		if(file.isFile())
			return FileType.FILE;
		else if(file.isDirectory())
			return FileType.DIRECTORY;
		else
			return FileType.UNKNOWN;
	}

	public synchronized File getFile() {
		return file;
	}

	public static enum FileType {
		FILE("file"),
		DIRECTORY("folder"),
		UNKNOWN("");
		
		private final String text;
		private String val;
		
		private FileType(String text) {
			this.text = text;
		}
		
		public static FileType parse(String type) {
			FileType fileType = FileType.UNKNOWN;
			for(FileType f : FileType.values()) {
				if(f.text.equals(type.toLowerCase())) {
					fileType = f;
					break;
				}
			}
			if(fileType.equals(FileType.UNKNOWN))
				fileType.val = type;
			return fileType;
		}
		
		@Override
		public String toString() {
			if(this.text.equals(""))
				return this.val;
			return this.text;
		}
	}
}
