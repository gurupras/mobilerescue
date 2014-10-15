package com.example.mobilerescue;


import java.io.File;

import android.app.Activity;
import android.app.Application;
import android.os.Environment;

public class AndroidApplication extends Application {
	public static File externalPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
	
	private static AndroidApplication instance;
	
    private FileEntry mRoot;
    private Activity mCurrentActivity;
    
    public AndroidApplication() {
    }
    
    public FileEntry getRoot() {
    	return mRoot;
    }
    
    public Activity getCurrentActivity() {
    	return mCurrentActivity;
    }
    
    public void setRoot(FileEntry root) {
    	this.mRoot = root;
    }
    
	public void setCurrentActivity(Activity activity) {
		this.mCurrentActivity = activity;
	}
	
	public static void init(AndroidApplication application) {
		instance = application;
	}

	public static AndroidApplication getInstance() {
		return instance;
	}
}
