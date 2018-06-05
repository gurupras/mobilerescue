package me.gurupras.androidcommons;

import android.app.Activity;
import android.widget.Toast;

public class Helper {
	public static String MAIN_TAG = "TAG";
	
	public static void makeToast(final String message) {
		Activity activity = AndroidApplication.getInstance().getCurrentActivity();
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(AndroidApplication.getInstance().getCurrentActivity(), 
						message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public static String pad(String string, int totalLength) {
		StringBuilder builder = new StringBuilder(string);
		int currentLength = builder.toString().length();
		int padLength = totalLength - currentLength;
		while(padLength-- > 0)
			builder.append(" ");
		return builder.toString();
	}
	
	/**
	 * Helps to create a new tag that is a combination of two tags
	 * @param mainTag {@code String}
	 * @param subTag {@code String}
	 * @return {@code String} combining the two tags
	 */
	public static String createTag(String mainTag, String subTag) {
		return mainTag + "->" + subTag;
	}
	
	/**
	 * Helps to create a new tag that is a combination of two tags
	 * @param c {@code Class} representing the new class for which to create a tag for
	 * @return {@code String} representing the new tag
	 */
	public static String createTag(Class c) {
		return MAIN_TAG + "->" + c.getName();
	}
	
	public static String toHex(byte[] arrayBytes) {
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString();
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
