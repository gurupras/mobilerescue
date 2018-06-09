package me.gurupras.mobilerescue;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import me.gurupras.androidcommons.AndroidApplication;
import me.gurupras.androidcommons.FileExplorerFragment;
import me.gurupras.androidcommons.FilesAdapter;
import me.gurupras.androidcommons.Helper;

public class MainActivity extends AppCompatActivity {
	TransferProgressDialog progressDialog;
	public static SharedPreferences settings;
	public static AndroidApplication application;
	public static DownloadService downloadService;
	
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
					.add(R.id.container, new RescueFileExplorerFragment(), "rescue").commit();
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
		createOptionsMenu();
		Helper.makeToast("Server: " + SettingsFragment.hostname);
		downloadService = new DownloadService();
		downloadService.start();
	}

	public boolean createOptionsMenu() {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);
		if(getSupportActionBar()!=null) {
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
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

		FileExplorerFragment fileExplorerFragment = (FileExplorerFragment) getSupportFragmentManager().findFragmentByTag("rescue");
		if (id == R.id.action_sort_name) {
			fileExplorerFragment.getCurrentRoot().build(FilesAdapter.SortType.NAME);
			fileExplorerFragment.refreshListView();
		}
		if (id == R.id.action_sort_size) {
			fileExplorerFragment.getCurrentRoot().build(FilesAdapter.SortType.SIZE);
			fileExplorerFragment.refreshListView();
		}
		if (id == R.id.action_sort_modified) {
			fileExplorerFragment.getCurrentRoot().build(FilesAdapter.SortType.LAST_MODIFIED);
			fileExplorerFragment.refreshListView();
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	public static class SettingsFragment extends Fragment {
		private static final String TAG = Helper.MAIN_TAG + "->Settings";
		private View rootView;
		private Activity activity;
		public static String hostname = "dirtydeeds.cse.buffalo.edu";
		public static int port = 30000;
		public static int serverPort = 13256;

		public SettingsFragment() {
			setRetainInstance(true);
			this.activity = AndroidApplication.getInstance().getCurrentActivity();
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			rootView = inflater.inflate(R.layout.fragment_settings, container, false);
			final EditText hostnameEditText = (EditText) rootView.findViewById(R.id.hostname_edittext);
			final EditText portEditText = (EditText) rootView.findViewById(R.id.port_edittext);
			final EditText serverPortEditText = (EditText) rootView.findViewById(R.id.serverPort_edittext);

			hostname = MainActivity.settings.getString("hostname", hostname);
			port = MainActivity.settings.getInt("port", port);
			serverPort = MainActivity.settings.getInt("serverPort", 13256);
			
			hostnameEditText.setText(hostname);
			portEditText.setText("" + port);
			serverPortEditText.setText("" + serverPort);
			
			rootView.setFocusableInTouchMode(true);
			rootView.requestFocus();
			rootView.setOnKeyListener(keyListener);
			hostnameEditText.setOnKeyListener(keyListener);
			portEditText.setOnKeyListener(keyListener);
			serverPortEditText.setOnKeyListener(keyListener);
			return rootView;
		}
		
		View.OnKeyListener keyListener = new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					EditText hostnameEditText = (EditText) rootView.findViewById(R.id.hostname_edittext);
					EditText portEditText = (EditText) rootView.findViewById(R.id.port_edittext);
					EditText serverPortEditText = (EditText) rootView.findViewById(R.id.serverPort_edittext);
					hostname = hostnameEditText.getText().toString();
					port = Integer.parseInt(portEditText.getText().toString());
					serverPort = Integer.parseInt(serverPortEditText.getText().toString());
					Log.d(TAG, "hostname :" + hostname + "@" + port);
					SharedPreferences.Editor editor = MainActivity.settings.edit();
					editor.putString("hostname", hostname);
					editor.putInt("port", port);
					editor.putInt("serverPort", serverPort);
					editor.commit();
					activity.onBackPressed();
					Helper.makeToast("Using: " + hostname + "@" + port);
					if(downloadService.port != serverPort) {
						downloadService.interrupt();
						downloadService = new DownloadService();
						downloadService.start();
					}
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
