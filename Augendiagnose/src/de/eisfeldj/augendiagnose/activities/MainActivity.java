package de.eisfeldj.augendiagnose.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import de.eisfeldj.augendiagnose.Application;
import de.eisfeldj.augendiagnose.R;
import de.eisfeldj.augendiagnose.util.DialogUtil;
import de.eisfeldj.augendiagnose.util.ImageUtil;
import de.eisfeldj.augendiagnose.util.MediaStoreUtil;
import de.eisfeldj.augendiagnose.util.PreferenceUtil;
import de.eisfeldj.augendiagnose.util.ReleaseNotesUtil;

/**
 * Main activity of the application.
 */
public class MainActivity extends Activity {

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SettingsActivity.setDefaultSharedPreferences(this);

		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_internal_organized_new_photo, false);

		Application.setLanguage();

		// Initial tip is triggered first, so that it is hidden behind release notes.
		DialogUtil.displayTip(this, R.string.message_tip_firstuse, R.string.key_tip_firstuse);

		Intent intent = getIntent();
		if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null) {
			// Application was started from other application by passing a list of images - open
			// OrganizeNewPhotosActivity.
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (imageUris != null) {
				ArrayList<String> fileNames = new ArrayList<String>();
				for (int i = 0; i < imageUris.size(); i++) {
					if (ImageUtil.getMimeType(imageUris.get(i)).startsWith("image/")) {
						String fileName = MediaStoreUtil.getRealPathFromUri(imageUris.get(i));
						if (fileName == null) {
							fileName = imageUris.get(i).getPath();
						}
						fileNames.add(fileName);
					}
				}
				boolean rightEyeLast = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_eye_sequence_choice);
				OrganizeNewPhotosActivity.startActivity(this, fileNames.toArray(new String[fileNames.size()]),
						PreferenceUtil.getSharedPreferenceString(R.string.key_folder_photos), rightEyeLast);
			}
		}

		if (Intent.ACTION_MAIN.equals(intent.getAction()) && savedInstanceState == null) {
			boolean firstStart = false;
			// When starting from launcher, check if started the first time in this version. If yes, display release
			// notes.
			String storedVersionString = PreferenceUtil.getSharedPreferenceString(R.string.key_internal_stored_version);
			if (storedVersionString == null || storedVersionString.length() == 0) {
				storedVersionString = "30";
				firstStart = true;
			}
			int storedVersion = Integer.parseInt(storedVersionString);
			int currentVersion = Application.getVersion();

			if (storedVersion < currentVersion) {
				ReleaseNotesUtil.displayReleaseNotes(this, firstStart, storedVersion + 1, currentVersion);
			}
		}

		if (!Application.isEyeFiInstalled()) {
			Button buttonEyeFi = (Button) findViewById(R.id.mainButtonOpenEyeFiApp);
			buttonEyeFi.setVisibility(View.GONE);
		}

		if (!Application.hasCameraActivity()) {
			Button buttonTakePhotos = (Button) findViewById(R.id.mainButtonTakePictures);
			buttonTakePhotos.setVisibility(View.GONE);
		}

		DialogUtil.checkOutOfMemoryError(this);

		test();
	}

	/*
	 * Inflate options menu.
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	/*
	 * Handle menu actions.
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			SettingsActivity.startActivity(this);
			break;
		case R.id.action_help:
			DisplayHtmlActivity.startActivity(this, R.string.html_overview);
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * onClick action for Button to open the Eye-Fi app.
	 *
	 * @param view
	 *            the button to open the Eye-Fi app.
	 */
	public final void openEyeFiApp(final View view) {
		if (Application.isEyeFiInstalled()) {
			startActivity(getPackageManager().getLaunchIntentForPackage("fi.eye.android"));
		}
		else {
			Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
			googlePlayIntent.setData(Uri.parse("market://details?id=fi.eye.android"));
			try {
				startActivity(googlePlayIntent);
			}
			catch (Exception e) {
				DialogUtil.displayError(this, R.string.message_dialog_eyefi_not_installed, false);
			}
		}
	}

	/**
	 * onClick action for Button to start the activity to take pictures.
	 *
	 * @param view
	 *            the button to take pictures.
	 */
	public final void takePicturesActivity(final View view) {
		TakePicturesActivity.startActivity(this, PreferenceUtil.getSharedPreferenceString(R.string.key_folder_input),
				PreferenceUtil.getSharedPreferenceBoolean(R.string.key_eye_sequence_choice));
	}

	/**
	 * onClick action for Button to display eye photos.
	 *
	 * @param view
	 *            the button to display the eye photos.
	 */
	public final void listFoldersForDisplayActivity(final View view) {
		ListFoldersForDisplayActivity.startActivity(this,
				PreferenceUtil.getSharedPreferenceString(R.string.key_folder_photos));
	}

	/**
	 * onClick action for Button to organize new eye photos.
	 *
	 * @param view
	 *            the button to organize new folders.
	 */
	public final void organizeNewFoldersActivity(final View view) {
		OrganizeNewPhotosActivity.startActivity(this,
				PreferenceUtil.getSharedPreferenceString(R.string.key_folder_input),
				PreferenceUtil.getSharedPreferenceString(R.string.key_folder_photos),
				PreferenceUtil.getSharedPreferenceBoolean(R.string.key_eye_sequence_choice));
	}

	/**
	 * Utility method - here it is possible to place code to be tested on startup.
	 */
	private void test() {
	}
}
