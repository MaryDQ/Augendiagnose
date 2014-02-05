package de.eisfeldj.augendiagnose;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;

/**
 * Utility class to retrieve base application resources
 */
public class Application extends android.app.Application {
	private static Context context;

	public void onCreate() {
		super.onCreate();
		Application.context = getApplicationContext();
	}

	/**
	 * Retrieve the application context
	 * 
	 * @return
	 */
	public static Context getAppContext() {
		return Application.context;
	}

	/**
	 * Retrieve the default display
	 * 
	 * @return
	 */
	public static Display getDefaultDisplay() {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		return wm.getDefaultDisplay();
	}

	/**
	 * Retrieve the max display size in pixels
	 * 
	 * @return
	 */
	public static int getDisplaySize() {
		Point p = new Point();
		getDefaultDisplay().getSize(p);
		return Math.max(p.x, p.y);
	}

	/**
	 * Retrieve the default shared preferences of the application
	 * 
	 * @return
	 */
	public static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Retrieve a String shared preference
	 * 
	 * @param preferenceId
	 * @return
	 */
	public static String getSharedPreferenceString(int preferenceId) {
		return getSharedPreferences().getString(context.getString(preferenceId), "");
	}

	/**
	 * Set a String shared preference
	 * 
	 * @param preferenceId
	 * @param s
	 */
	public static void setSharedPreferenceString(int preferenceId, String s) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(context.getString(preferenceId), s);
		editor.commit();
	}

	/**
	 * Retrieve a boolean shared preference
	 * 
	 * @param preferenceId
	 * @return
	 */
	public static boolean getSharedPreferenceBoolean(int preferenceId) {
		return getSharedPreferences().getBoolean(context.getString(preferenceId), false);
	}

	/**
	 * Check if the application has an authorized user key
	 * 
	 * @return
	 */
	public static boolean isAuthorized() {
		String userKey = getSharedPreferenceString(R.string.key_user_key);
		return userKey.equals("Schnurpsi");
	}
}