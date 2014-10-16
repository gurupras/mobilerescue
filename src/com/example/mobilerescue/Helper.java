package com.example.mobilerescue;

import android.app.Activity;
import android.widget.Toast;

public class Helper {
	public static final String MAIN_TAG = "MobileRescue";
	
	public static void makeToast(final String message) {
		Activity activity = AndroidApplication.getInstance().getCurrentActivity();
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(AndroidApplication.getInstance().getCurrentActivity(), 
						message, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
