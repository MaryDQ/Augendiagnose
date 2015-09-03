package de.jeisfeld.augendiagnoselib.util.imagefile;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import de.jeisfeld.augendiagnoselib.Application;
import de.jeisfeld.augendiagnoselib.R;
import de.jeisfeld.augendiagnoselib.util.DateUtil;

/**
 * Utility class for operations with images.
 */
public final class ImageUtil {
	// JAVADOC:OFF
	// Rotation angles
	private static final int ROTATION_90 = 90;
	private static final int ROTATION_180 = 180;
	private static final int ROTATION_270 = 270;

	// JAVADOC:ON

	/**
	 * Number of milliseconds for retry of getting bitmap.
	 */
	private static final long BITMAP_RETRY = 50;

	/**
	 * The file endings considered as image files.
	 */
	private static final List<String> IMAGE_SUFFIXES = Arrays.asList(
			new String[] { "JPG", "JPEG", "PNG", "BMP", "TIF", "TIFF", "GIF" });

	/**
	 * Hide default constructor.
	 */
	private ImageUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the date field with the EXIF date from the file If not existing, use the last modified date.
	 *
	 * @param path
	 *            The file path of the image
	 * @return the date stored in the EXIF data.
	 */
	public static Date getExifDate(final String path) {
		Date retrievedDate = null;
		try {
			ExifInterface exif = new ExifInterface(path);
			String dateString = exif.getAttribute(ExifInterface.TAG_DATETIME);

			retrievedDate = DateUtil.parse(dateString, "yyyy:MM:dd HH:mm:ss");
		}
		catch (Exception e) {
			Log.w(Application.TAG, e.toString() + " - Cannot retrieve EXIF date for " + path);
		}
		if (retrievedDate == null) {
			File f = new File(path);
			retrievedDate = new Date(f.lastModified());
		}
		return retrievedDate;
	}

	/**
	 * Retrieve the rotation angle from the Exif data of an image.
	 *
	 * @param path
	 *            The file path of the image
	 * @return the rotation stored in the exif data, mapped into degrees.
	 */
	public static int getExifRotation(final String path) {
		int rotation = 0;
		try {
			ExifInterface exif = new ExifInterface(path);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

			if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
				// Use custom implementation, as the previous one is not always reliable
				orientation = JpegMetadataUtil.getExifOrientation(new File(path));
			}

			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotation = ROTATION_270;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotation = ROTATION_180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotation = ROTATION_90;
				break;
			default:
				break;
			}
		}
		catch (Exception e) {
			Log.w(Application.TAG, "Exception when getting EXIF rotation");
		}
		return rotation;
	}

	/**
	 * Return a bitmap of this photo.
	 *
	 * @param path
	 *            The file path of the image.
	 * @param maxSize
	 *            The maximum size of this bitmap. If bigger, it will be resized.
	 * @return the bitmap.
	 */
	public static Bitmap getImageBitmap(final String path, final int maxSize) {
		Bitmap bitmap = null;

		if (maxSize <= 0) {
			bitmap = BitmapFactory.decodeFile(path);
		}
		else {

			if (maxSize <= MediaStoreUtil.MINI_THUMB_SIZE) {
				bitmap = MediaStoreUtil.getThumbnailFromPath(path, maxSize);
			}

			if (bitmap == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = getBitmapFactor(path, maxSize);
				// options.inPurgeable = true;
				bitmap = BitmapFactory.decodeFile(path, options);
				if (bitmap == null) {
					// cannot create bitmap - try once more in case that the image was just in process of saving
					// metadata
					try {
						Thread.sleep(BITMAP_RETRY);
					}
					catch (InterruptedException e) {
						// ignore exception
					}
					bitmap = BitmapFactory.decodeFile(path, options);

					if (bitmap == null) {
						// cannot create bitmap - return dummy
						Log.w(Application.TAG, "Cannot create bitmap from path " + path + " - return dummy bitmap");
						return getDummyBitmap();
					}
				}
			}
			if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
				return bitmap;
			}

			if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize
					|| maxSize <= MediaStoreUtil.MINI_THUMB_SIZE) {
				// Only if bitmap is bigger than maxSize, then resize it - but don't trust the thumbs from media store.
				if (bitmap.getWidth() > bitmap.getHeight()) {
					int targetWidth = maxSize;
					int targetHeight = bitmap.getHeight() * maxSize / bitmap.getWidth();
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
				else {
					int targetWidth = bitmap.getWidth() * maxSize / bitmap.getHeight();
					int targetHeight = maxSize;
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
			}

		}

		int rotation = getExifRotation(path);
		if (rotation != 0) {
			bitmap = rotateBitmap(bitmap, rotation);
		}

		return bitmap;
	}

	/**
	 * Return a bitmap of a photo directly from byte array data.
	 *
	 * @param data
	 *            The byte array data of the bitmap.
	 * @param maxSize
	 *            The maximum size of this bitmap. If bigger, it will be resized.
	 * @return the bitmap.
	 */
	public static Bitmap getImageBitmap(final byte[] data, final int maxSize) {
		Bitmap bitmap = null;

		if (maxSize <= 0) {
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		}
		else {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = getBitmapFactor(data, maxSize);
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

			if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
				return bitmap;
			}

			if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
				// Only if bitmap is bigger than maxSize, then resize it
				if (bitmap.getWidth() > bitmap.getHeight()) {
					int targetWidth = maxSize;
					int targetHeight = bitmap.getHeight() * maxSize / bitmap.getWidth();
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
				else {
					int targetWidth = bitmap.getWidth() * maxSize / bitmap.getHeight();
					int targetHeight = maxSize;
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
			}
		}

		return bitmap;
	}

	/**
	 * Retrieve a part of a bitmap in full resolution.
	 *
	 * @param fullBitmap
	 *            The bitmap from which to get the part.
	 * @param minX
	 *            The minimum X position to retrieve.
	 * @param maxX
	 *            The maximum X position to retrieve.
	 * @param minY
	 *            The minimum Y position to retrieve.
	 * @param maxY
	 *            The maximum Y position to retrieve.
	 * @return The bitmap.
	 */
	public static Bitmap getPartialBitmap(final Bitmap fullBitmap, final float minX, final float maxX,
			final float minY,
			final float maxY) {
		Bitmap partialBitmap =
				Bitmap.createBitmap(fullBitmap, Math.round(minX * fullBitmap.getWidth()),
						Math.round(minY * fullBitmap.getHeight()),
						Math.round((maxX - minX) * fullBitmap.getWidth()),
						Math.round((maxY - minY) * fullBitmap.getHeight()));

		return partialBitmap;
	}

	/**
	 * Utility to retrieve the sample size for BitmapFactory.decodeFile.
	 *
	 * @param filepath
	 *            the path of the bitmap.
	 * @param targetSize
	 *            the target size of the bitmap
	 * @return the sample size to be used.
	 */
	private static int getBitmapFactor(final String filepath, final int targetSize) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filepath, options);
		int size = Math.max(options.outWidth, options.outHeight);
		return size / targetSize;
	}

	/**
	 * Utility to retrieve the sample size for BitmapFactory.decodeFile.
	 *
	 * @param data
	 *            the data of the bitmap.
	 * @param targetSize
	 *            the target size of the bitmap
	 * @return the sample size to be used.
	 */
	private static int getBitmapFactor(final byte[] data, final int targetSize) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);
		int size = Math.max(options.outWidth, options.outHeight);
		return size / targetSize;
	}

	/**
	 * Rotate a bitmap.
	 *
	 * @param source
	 *            The original bitmap
	 * @param angle
	 *            The rotation angle
	 * @return the rotated bitmap.
	 */
	public static Bitmap rotateBitmap(final Bitmap source, final float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	/**
	 * Get Mime type from URI.
	 *
	 * @param uri
	 *            The URI
	 * @return the mime type.
	 */
	public static String getMimeType(final Uri uri) {
		ContentResolver contentResolver = Application.getAppContext().getContentResolver();
		String mimeType = contentResolver.getType(uri);
		if (mimeType == null) {
			String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			if (extension != null) {
				extension = extension.toLowerCase(Locale.getDefault());
			}
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mimeType == null) {
				mimeType = "unknown";
			}
		}
		return mimeType;
	}

	/**
	 * Check if a file is an image file.
	 *
	 * @param file
	 *            The file
	 * @param strict
	 *            if true, then the file content will be checked, otherwise the suffix is sufficient.
	 * @return true if it is an image file.
	 */
	public static boolean isImage(final File file, final boolean strict) {
		if (file == null || !file.exists() || file.isDirectory()) {
			return false;
		}
		if (!strict) {
			String fileName = file.getName();
			int index = fileName.lastIndexOf('.');
			if (index >= 0) {
				String suffix = fileName.substring(index + 1);
				if (IMAGE_SUFFIXES.contains(suffix.toUpperCase(Locale.getDefault()))) {
					return true;
				}
			}
		}
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getPath(), options);
		return options.outWidth >= 0 - 1 && options.outHeight >= 0;
	}

	/**
	 * Get the list of image files in a folder.
	 *
	 * @param folderName
	 *            The folder name.
	 * @return The list of image files in this folder.
	 */
	public static ArrayList<String> getImagesInFolder(final String folderName) {
		ArrayList<String> fileNames = new ArrayList<String>();
		if (folderName == null) {
			return fileNames;
		}
		File folder = new File(folderName);
		if (!folder.exists() || !folder.isDirectory()) {
			return fileNames;
		}
		File[] imageFiles = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return isImage(file, false);
			}
		});
		if (imageFiles == null) {
			return fileNames;
		}

		for (File file : imageFiles) {
			fileNames.add(file.getAbsolutePath());
		}
		return fileNames;
	}

	/**
	 * Retrieves a dummy bitmap (for the case that an image file is not readable).
	 *
	 * @return the dummy bitmap.
	 */
	public static Bitmap getDummyBitmap() {
		return BitmapFactory.decodeResource(Application.getAppContext().getResources(), R.drawable.cannot_read_image);
	}

	/**
	 * File filter class to identify image files.
	 */
	public static class ImageFileFilter implements FileFilter {
		@Override
		public final boolean accept(final File file) {
			Uri uri = Uri.fromFile(file);
			return file.exists() && file.isFile() && ImageUtil.getMimeType(uri).startsWith("image/");
		}

	}

}