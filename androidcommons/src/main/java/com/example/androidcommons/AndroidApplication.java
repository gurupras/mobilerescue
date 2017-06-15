package com.example.androidcommons;


import android.app.Activity;
import android.app.Application;
import android.os.Environment;

import java.io.File;

public class AndroidApplication extends Application {
	public static File externalPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
	//public static File externalPath = new File("/sdcard");
	
	private static AndroidApplication instance;
    private Activity mCurrentActivity;
    
    public AndroidApplication() { }

    public Activity getCurrentActivity() { return mCurrentActivity; }
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
