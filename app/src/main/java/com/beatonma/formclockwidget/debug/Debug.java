package com.beatonma.formclockwidget.debug;

/**
 * Created by Michael on 04/01/2016.
 *
 * Occasionally useful for toggling stuff while testing.
 */
public class Debug {
	public static boolean DEBUG = true;

	public static boolean toggle() {
		DEBUG = !DEBUG;
		return DEBUG;
	}
}
