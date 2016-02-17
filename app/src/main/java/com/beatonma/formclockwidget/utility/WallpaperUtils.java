package com.beatonma.formclockwidget.utility;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.beatonma.colorpicker.ColorContainer;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.google.android.apps.muzei.api.MuzeiContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Michael on 28/10/2015.
 */
public class WallpaperUtils {
	private final static String TAG = "WallpaperUtils";

	private final static String LWP_MUZEI = "net.nurik.roman.muzei";
	private final static String LWP_REGISTER = "registered_lwp";
	private final static String MUZEI_SOURCE_ZYDEN = "cloud-walls.com/wallpapers/zyden/Zyden/";

    public final static int PALETTE_DEFAULT = 0;
	public final static int PALETTE_PREFER_DARK = 1;
	public final static int PALETTE_PREFER_LIGHT = 2;

	public static boolean isMuzei(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		return (info != null && info.getPackageName().equals(LWP_MUZEI));
	}

	public static Bitmap getWallpaperBitmap(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

		Drawable wallpaperDrawable;
		Bitmap wallpaperBitmap = null;
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		if (info != null) {
			// wallpaper is live
			String wallpaperPackage = info.getPackageName();

			if (wallpaperPackage.equals(LWP_MUZEI)) {
				try {
					wallpaperBitmap = getMuzeiImage(context);
				}
				catch (Exception e) {
					Log.e(TAG, "Error getting Muzei bitmap.");
					e.printStackTrace();
				}

				if (wallpaperBitmap != null) {
					return wallpaperBitmap;
				}
			}

			// default to LWP thumbnail
			PackageManager pm = context.getPackageManager();
			wallpaperDrawable = info.loadThumbnail(pm);
		}
		else {
			// wallpaper is a static image
			wallpaperDrawable = wallpaperManager.getFastDrawable();
		}

		wallpaperBitmap = Utils.drawableToBitmap(wallpaperDrawable);

		return wallpaperBitmap;
	}

	public static Bitmap getMuzeiImage(Context context) {
		InputStream is = null;
		Bitmap output = null;
		ContentResolver resolver = context.getContentResolver();

		try {
			is = resolver.openInputStream(MuzeiContract.Artwork.CONTENT_URI);
		}
		catch (Exception e) {
			Log.e(TAG, "Error opening input stream from Muzei: " + e.toString());
			e.printStackTrace();
		}

		// Start Zyden workaround
		// Workaround to handle Zyden wallpapers. Something about the formatting of certain files
		// causes them to be converted to greyscale. Caching the file locally and opening an
		// inputstream from that file seems to work. I'm not sure why!
		Cursor cursor = resolver.query(MuzeiContract.Artwork.CONTENT_URI, new String[] {"title", "token"}, null, null, null);
		cursor.moveToFirst();
		String title = cursor.getString(0);
		String token = cursor.getString(1);
		cursor.close();

		if (token != null && token.contains(MUZEI_SOURCE_ZYDEN)) {
			Log.d(TAG, "Attempting to use Zyden workaround");
			File cacheFile = new File(context.getFilesDir(), title);
			if (!cacheFile.exists()) {
				if (is != null) {
					try { // Cache to local file
						Bitmap bitmap = BitmapFactory.decodeStream(is);
						writeBitmapToFile(context, title, bitmap);
						bitmap.recycle();
					}
					catch (OutOfMemoryError e) {
						Log.e(TAG, "Out of memory trying to cache Zyden bitmap: " + e.toString());
					}
					catch (Exception e) {
						Log.e(TAG, "Error trying to cache Zyden wallpaper: " + e.toString());
					}
				}
			}

			try {
				is = new FileInputStream(cacheFile);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		// End Zyden workaround

		if (is != null) {
			try {
				BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
				int maxTextureSize = Utils.getMaxTextureSize();
				int n = (int) (maxTextureSize * 0.30);
				int imageWidth = decoder.getWidth();
				int imageHeight = decoder.getHeight();

				Rect rect;
				if (imageHeight > maxTextureSize * 0.8 || imageWidth > maxTextureSize * 0.8) {
					rect = new Rect((imageWidth / 2) - n, (imageHeight / 2) - n, (imageWidth / 2) + n, (imageHeight / 2) + n);
				}
				else {
					rect = new Rect(0, 0, imageWidth, imageHeight);
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				output = decoder.decodeRegion(rect, options);
			}
			catch (OutOfMemoryError e) {
				Log.e(TAG, "Out of memory - image too large: " + e.toString());
				e.printStackTrace();
				output = null;
			}
			catch (Exception e) {
				Log.e(TAG, "Error getting bitmap region decoder: " + e.toString());
				output = null;
			}
		}
		else {
			Log.e(TAG, "Muzei input stream is null.");
			return null;
		}
		return output;
	}

	public static File writeBitmapToFile(Context context, String filename, Bitmap bitmap) {
		FileOutputStream out = null;
		Log.d(TAG, "writing to file");
		filename = context.getFilesDir() + File.separator + filename;
		try {
			out = new FileOutputStream(filename);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new File(filename);
	}

	public static void extractColors(Context context, Bitmap bitmap) {
		Palette palette;
		try {
			palette = Palette.from(bitmap)
					.maximumColorCount(16)
					.generate();
		}
		catch (Exception e) {
			Log.e(TAG, "Error building Palette from bitmap: " + e.toString());
			return;
		}

		int color1, color2, color3,
				colorShadow, colorComplications;

        int colorBias = PrefUtils.get(context).getInt(PrefUtils.PREF_COLOR_BIAS, PALETTE_PREFER_DARK);

        color1 = getFirstColor(palette, colorBias);
        color2 = getSecondColor(palette, colorBias);
        color3 = getThirdColor(palette, colorBias, isMuzei(context));

		if (color1 == Color.BLACK) {
			color1 = lighten(color2, 0.2f);
		}

		List<Palette.Swatch> swatches = palette.getSwatches();
		int mostCommon = 0;
		if (swatches.isEmpty()) {
			colorShadow = Color.BLACK;
			colorComplications = Color.WHITE;
		}
		else {
			for (int i = 0; i < swatches.size(); i++) {
				Palette.Swatch s = swatches.get(i);
				if (s.getPopulation() > mostCommon) {
					mostCommon = i;
				}
			}

			Palette.Swatch largestSwatch = swatches.get(mostCommon);
			colorShadow = flatten(largestSwatch.getTitleTextColor());
			colorComplications = getContrasting(colorShadow);
		}

		colorShadow = Color.argb(100, Color.red(colorShadow), Color.green(colorShadow), Color.blue(colorShadow));

		SharedPreferences sp = context.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
		ColorContainer container1 = ColorUtils.getColorContainerFromPreference(sp, PrefUtils.PREF_COLOR1);
		ColorContainer container2 = ColorUtils.getColorContainerFromPreference(sp, PrefUtils.PREF_COLOR2);
		ColorContainer container3 = ColorUtils.getColorContainerFromPreference(sp, PrefUtils.PREF_COLOR3);
		ColorContainer containerShadow = ColorUtils.getColorContainerFromPreference(sp, PrefUtils.PREF_COLOR_SHADOW);
		ColorContainer containerComplications = ColorUtils.getColorContainerFromPreference(sp, PrefUtils.PREF_COLOR_COMPLICATIONS);

		container1.setFromWallpaper(color1);
		container2.setFromWallpaper(color2);
		container3.setFromWallpaper(color3);
		containerShadow.setFromWallpaper(colorShadow);
		containerComplications.setFromWallpaper(colorComplications);

		ColorUtils.writeColorContainerToPreference(sp, PrefUtils.PREF_COLOR1, container1);
		ColorUtils.writeColorContainerToPreference(sp, PrefUtils.PREF_COLOR2, container2);
		ColorUtils.writeColorContainerToPreference(sp, PrefUtils.PREF_COLOR3, container3);
		ColorUtils.writeColorContainerToPreference(sp, PrefUtils.PREF_COLOR_SHADOW, containerShadow);
		ColorUtils.writeColorContainerToPreference(sp, PrefUtils.PREF_COLOR_COMPLICATIONS, containerComplications);
	}

    private static int getFirstColor(Palette palette, int colorBias) {
        int defaultColor = Color.BLACK;
        int output = defaultColor;

        switch (colorBias) {
            case PALETTE_DEFAULT:
                output = palette.getLightMutedColor(
                        palette.getLightVibrantColor(
                                palette.getMutedColor(defaultColor)));
                break;
            case PALETTE_PREFER_DARK:
                output = palette.getDarkMutedColor(
                        palette.getMutedColor(defaultColor));
                break;
            case PALETTE_PREFER_LIGHT:
                output = palette.getLightMutedColor(
                        palette.getLightVibrantColor(defaultColor));
                break;
        }

        return output;
    }

    private static int getSecondColor(Palette palette, int colorBias) {
        int defaultColor = Color.GRAY;
        int output = defaultColor;

        switch (colorBias) {
            case PALETTE_DEFAULT:
                output = palette.getVibrantColor(
                        palette.getDarkVibrantColor(
                                palette.getMutedColor(
                                        palette.getDarkMutedColor(defaultColor))));
                break;
            case PALETTE_PREFER_DARK:
                output = palette.getDarkVibrantColor(defaultColor);
                break;
            case PALETTE_PREFER_LIGHT:
                output = palette.getLightVibrantColor(
                        palette.getMutedColor(defaultColor));
                break;
        }

        return output;
    }

    private static int getThirdColor(Palette palette, int colorBias, boolean isMuzei) {
        int defaultColor = Color.WHITE;
        int output = defaultColor;

        switch (colorBias) {
            case PALETTE_DEFAULT:
                if (isMuzei) {
                    output = Color.WHITE;
                }
                else {
                    output = palette.getDarkVibrantColor(defaultColor);
                }
                break;
            case PALETTE_PREFER_DARK:
                output = palette.getMutedColor(Color.DKGRAY);
                break;
            case PALETTE_PREFER_LIGHT:
                output = palette.getVibrantColor(defaultColor);
                break;
        }

        return output;
    }

	// Remove transparency
	private static int flatten(int color) {
		return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
	}

	// If this color is dark return white; if light return black
	private static int getContrasting(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);

		if (hsv[2] > 0.5f) {
			return Color.BLACK;
		}
		else {
			return Color.WHITE;
		}
	}

	private static int lighten(int color, float amount) {
		float hsv[] = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] = Math.max(0f, Math.min(1f, hsv[2] + amount));
		return Color.HSVToColor(hsv);
	}

	public static int getThemeFromWallpaper(SharedPreferences preferences) {
		int[] themes = { R.style.AppTheme,
				R.style.Grey, R.style.Red, R.style.DeepOrange, R.style.Amber,
				R.style.Green, R.style.Blue, R.style.DeepPurple, R.style.Pink};
		return themes[whatColor(
				ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR1).getColor()
		)];
	}

	public static int whatColor(int color) {
		float[] hsv = new float[3];
		int result;

		Color.colorToHSV(color, hsv);

		if (hsv[1] == 0) {
			// GREY
			result = 1;
		}
		else {
			float hue = hsv[0];

			if (hue <= 14f) {
				// RED
				result = 2;
			}
			else if (hue <= 33f) {
				// ORANGE
				result = 3;
			}
			else if (hue <= 70f) {
				// YELLOW
				result = 4;
			}
			else if (hue <= 180) {
				// GREEN
				result = 5;
			}
			else if (hue <= 260f) {
				// BLUE
				result = 6;
			}
			else if (hue <= 300f) {
				// PURPLE
				result = 7;
			}
			else if (hue <= 360f) {
				// PINK
				result = 8;
			}
			else {
				// who knows?
				result = 0;
			}
		}

		return result;
	}
}
