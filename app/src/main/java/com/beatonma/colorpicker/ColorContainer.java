package com.beatonma.colorpicker;

import android.graphics.Color;

/**
 * Created by Michael on 07/05/2015.
 *
 * Colors can come from several sources.
 * - Preset swatch/color position
 * - Custom raw Color value
 * - Selected automatically from a wallpaper using Palette
 *
 * ColorContainer tracks these sources. If a user 'locks' a color then their selection will trump
 * the wallpaper color.
 */
public class ColorContainer {
    private final static String TAG = "ColorContainer";

	// Preset swatch id
    int swatch = -1;

	// Position of chosen color in swatch
    int color = -1;

	// Raw Color value
    int colorValue = -1;

	// Color pulled from wallpaper palette
	int fromWallpaper = -1;

	boolean isDefault = false;

	// If locked, prefer user-chosen color over wallpaper palette-chosen color
    boolean locked = false;

	public ColorContainer() {
		this(Color.BLACK);
		isDefault = true;
	}

    public ColorContainer(int swatch, int color) {
        this.swatch = swatch;
        this.color = color;
        this.colorValue = ColorUtils.getColor(swatch, color + 1);
    }

	public ColorContainer(int swatch, int color, boolean locked) {
		this.swatch = swatch;
		this.color = color;
		this.colorValue = ColorUtils.getColor(swatch, color + 1);
		this.locked = locked;
	}

    public ColorContainer(int swatch, int color, int colorValue) {
        this.swatch = swatch;
        this.color = color;
        this.colorValue = colorValue;
		this.locked = false;
    }

    public ColorContainer(int colorValue) {
        this.swatch = -1;
        this.color = -1;
        this.colorValue = colorValue;
		this.locked = false;
    }

    public ColorContainer(String container) {
        String[] parts = container.split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.contains("swatch=")) {
                this.swatch = Integer.valueOf(part.split("=")[1]);
            }
            else if (part.contains("color=")) {
                this.color = Integer.valueOf(part.split("=")[1]);
            }
            else if (part.contains("color_value=")) {
                this.colorValue = Integer.valueOf(part.split("=")[1]);
            }
			else if (part.contains("locked=")) {
				this.locked = Boolean.valueOf(part.split("=")[1]);
			}
			else if (part.contains("from_wallpaper=")) {
				this.fromWallpaper = Integer.valueOf(part.split("=")[1]);
			}
        }

		if (this.colorValue == -1 && this.swatch >= 0 && this.color >= 0) {
			this.colorValue = ColorUtils.getColor(swatch, color + 1);
		}
    }

    public boolean isCustom() {
        return (swatch == -1 && color == -1);
    }

	public void setCustomColor(int color) {
		this.colorValue = color;
		this.swatch = -1;
		this.color = -1;
	}

	public void setPresetColor(int swatch, int color) {
		this.swatch = swatch;
		this.color = color;
		this.colorValue = ColorUtils.getColor(swatch, color + 1);
	}

    @Override
    public String toString() {
        return "swatch=" + swatch
				+ ";color=" + color
				+ ";color_value=" + colorValue
				+ ";locked=" + locked
				+ ";from_wallpaper=" + fromWallpaper;
    }

    @Override
    public boolean equals(Object other) {
		return (other instanceof ColorContainer
				&& this.toString().equals(other.toString()));
    }

	public int getColor() {
		if (locked) {
			return colorValue;
		}
		else {
			return fromWallpaper;
		}
	}

	public int getChosenColor() {
		return colorValue;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public int getFromWallpaper() {
		return fromWallpaper;
	}

	public void setFromWallpaper(int color) {
		this.fromWallpaper = color;
	}

	public boolean isDefault() {
		return isDefault;
	}
}
