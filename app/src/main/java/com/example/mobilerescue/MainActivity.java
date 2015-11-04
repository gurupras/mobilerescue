package com.example.mobilerescue;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.os.Build;

import com.example.androidcommons.AndroidApplication;
import com.example.androidcommons.Helper;

import static com.example.androidcommons.Helper.makeToast;

public class MainActivity extends ActionBarActivity {
	ProgressDialog progressDialog;
	public static SharedPreferences settings;
	public static AndroidApplication application;
	private static String TAG = "MobileRescue";

	static {
		Helper.MAIN_TAG = TAG;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new RescueFileExplorerFragment()).commit();
		}
		
		AndroidApplication.init((AndroidApplication) getApplication());
		application = AndroidApplication.getInstance();
		application.setCurrentActivity(this);

		settings = getSharedPreferences("settings", MODE_PRIVATE);
		try {
			SettingsFragment.hostname = settings.getString("hostname", SettingsFragment.hostname);
			SettingsFragment.port = settings.getInt("port", SettingsFragment.port);
		} catch(Exception e) {
			Log.e(TAG, "Unhandled exception :" + e);
			e.printStackTrace();
		}
		Helper.makeToast("Server: " + SettingsFragment.hostname);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.container, new SettingsFragment(), "Settings").addToBackStack("Settings").commit();
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	public static class SettingsFragment extends Fragment {
		private static final String TAG = Helper.MAIN_TAG + "->Settings";
		private View rootView;
		private Activity activity;
		public static String hostname = "dirtydeeds.cse.buffalo.edu";
		public static int port = 30000;

		public SettingsFragment() {
			setRetainInstance(true);
			this.activity = AndroidApplication.getInstance().getCurrentActivity();
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			rootView = inflater.inflate(R.layout.fragment_settings, container, false);
			final EditText hostnameEditText = (EditText) rootView.findViewById(R.id.hostname_edittext);
			final EditText portEditText = (EditText) rootView.findViewById(R.id.port_edittext);

			hostname = MainActivity.settings.getString("hostname", hostname);
			port = MainActivity.settings.getInt("port", port);

			hostnameEditText.setText(hostname);
			portEditText.setText("" + port);
			rootView.setFocusableInTouchMode(true);
			rootView.requestFocus();
			rootView.setOnKeyListener(keyListener);
			hostnameEditText.setOnKeyListener(keyListener);
			portEditText.setOnKeyListener(keyListener);
			return rootView;
		}
		
		View.OnKeyListener keyListener = new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					EditText hostnameEditText = (EditText) rootView.findViewById(R.id.hostname_edittext);
					EditText portEditText = (EditText) rootView.findViewById(R.id.port_edittext);
					hostname = hostnameEditText.getText().toString();
					port = Integer.parseInt(portEditText.getText().toString());
					Log.d(TAG, "hostname :" + hostname + "@" + port);
					SharedPreferences.Editor editor = MainActivity.settings.edit();
					editor.putString("hostname", hostname);
					editor.putInt("port", port);
					editor.commit();
					activity.onBackPressed();
					Helper.makeToast("Using: " + hostname + "@" + port);
					return true;
				}
				return false;
			}
		};
	}
	
	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		if(fm.getBackStackEntryCount() > 0) {
			fm.popBackStack();
//			fm.beginTransaction().replace(R.id.container, new FileExplorerFragment()).commit();
		}
		else
			super.onBackPressed();
	}
}
