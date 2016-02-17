package com.beatonma.formclockwidget.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Michael on 08/02/2016.
 */
public class DbHelper extends SQLiteOpenHelper {
    private final static String TAG = "DbHelper";

    private final static int DB_VERSION = 1;

    private final static String DB_MAIN = "formclock";
    private final static String TABLE_SHORTCUTS = "widget_shortcuts";

    private final static String FIELD_PACKAGE_NAME = "package_name";
    private final static String FIELD_ACTIVITY_NAME = "activity_name";
    private final static String FIELD_FRIENDLY_NAME = "friendly_name";
    private final static String FIELD_SELECTED = "selected";

    private final static int FALSE = 0;
    private final static int TRUE = 1;

    private static DbHelper mInstance;

    public static DbHelper getInstance(Context context) {
        mInstance = new DbHelper(context);
        return mInstance;
    }

    private DbHelper(Context context) {
        super(context, DB_MAIN, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createShortcutsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, String.format("Upgrading database from version %d to %d. THIS WILL DESTROY OLD DATA", oldVersion, newVersion));
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHORTCUTS);

        onCreate(db);
    }

    private void createShortcutsTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_SHORTCUTS
                        + " ("
                        + FIELD_PACKAGE_NAME + " TEXT NOT NULL PRIMARY KEY, "
                        + FIELD_ACTIVITY_NAME + " TEXT NOT NULL, "
                        + FIELD_FRIENDLY_NAME + " TEXT NOT NULL, "
                        + FIELD_SELECTED + " INTEGER NOT NULL"
                        + ");");
    }

    private void resetShortcuts(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHORTCUTS);
        createShortcutsTable(db);
    }

    public boolean updateShortcuts(ArrayList<AppContainer> apps) {
        SQLiteDatabase db = getWritableDatabase();

        resetShortcuts(db);

        boolean allSuccess = true;

        db.beginTransaction();
        for (AppContainer app : apps) {
            boolean success = updateShortcut(db, app);
            if (!success) {
                allSuccess = false;
            }
        }

        if (allSuccess) {
            db.setTransactionSuccessful();
            Log.v(TAG, String.format("Notification subscriptions updated successfully (%d items saved)", apps.size()));
        }
        else {
            Log.v(TAG, "Notification subscriptions could not be updated.");
        }

        db.endTransaction();
        db.close();

        return allSuccess;
    }

    public boolean updateShortcut(SQLiteDatabase db, AppContainer app) {
        ContentValues values = new ContentValues();
        values.put(FIELD_PACKAGE_NAME, app.getPackageName());
        values.put(FIELD_ACTIVITY_NAME, app.getActivityName());
        values.put(FIELD_FRIENDLY_NAME, app.getFriendlyName());
        values.put(FIELD_SELECTED, (app.isChecked() ? TRUE : FALSE));

        boolean success = db.insertWithOnConflict(TABLE_SHORTCUTS, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;

        return success;
    }

    public boolean updateSingleShortcut(AppContainer app) {
        ContentValues values = new ContentValues();
        values.put(FIELD_PACKAGE_NAME, app.getPackageName());
        values.put(FIELD_ACTIVITY_NAME, app.getActivityName());
        values.put(FIELD_FRIENDLY_NAME, app.getFriendlyName());
        values.put(FIELD_SELECTED, (app.isChecked() ? TRUE : FALSE));

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        boolean success = db.insertWithOnConflict(TABLE_SHORTCUTS, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
        if (success) {
            db.setTransactionSuccessful();
        }

        db.endTransaction();
        db.close();

        return success;
    }

    public boolean removeShortcut(AppContainer app) {
        String packageName = app.getPackageName();
        ContentValues values = new ContentValues();
        values.put(FIELD_PACKAGE_NAME, packageName);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        boolean success = db.delete(TABLE_SHORTCUTS, FIELD_PACKAGE_NAME + " = \'" + packageName + "\'", null) == 1;

        if (success) {
            db.setTransactionSuccessful();
            Log.v(TAG, String.format("Successfully removed notification %s", packageName));
        }
        else {
            Log.v(TAG, String.format("Could not remove notification %s", packageName));
        }
        db.endTransaction();
        db.close();

        return success;
    }

    public AppContainer findShortcut(String packageName) {
        AppContainer app = null;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();

        String columns[] = new String[] { FIELD_PACKAGE_NAME, FIELD_ACTIVITY_NAME, FIELD_FRIENDLY_NAME, FIELD_SELECTED };

        Cursor cursor = db.query(TABLE_SHORTCUTS, columns, getWhereClause(FIELD_PACKAGE_NAME, packageName), null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            app = new AppContainer();
            app.setPackageName(cursor.getString(0));
            app.setActivityName(cursor.getString(1));
            app.setFriendlyName(cursor.getString(2));
            app.setChecked(cursor.getInt(3) == TRUE);
            cursor.close();
            db.setTransactionSuccessful();
        }
        db.endTransaction();
        db.close();

        return app;
    }

    public ArrayList<AppContainer> getShortcuts() {
        ArrayList<AppContainer> subscriptions = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            db.beginTransaction();

            String columns[] = new String[]{FIELD_PACKAGE_NAME, FIELD_ACTIVITY_NAME, FIELD_FRIENDLY_NAME, FIELD_SELECTED};

            Cursor cursor = db.query(TABLE_SHORTCUTS, columns, null, null, null, null, null);
            if (cursor != null) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    AppContainer app = new AppContainer();
                    app.setPackageName(cursor.getString(0));
                    app.setActivityName(cursor.getString(1));
                    app.setFriendlyName(cursor.getString(2));
                    app.setChecked(cursor.getInt(3) == TRUE);

                    subscriptions.add(app);
                }
                cursor.close();
            }
            db.endTransaction();
            db.close();
        }
        catch (Exception e) {
            Log.e(TAG, "Error getting subscriptions: " + e.toString());
        }
        return subscriptions;
    }

    private String getWhereClause(String fieldName, String value) {
        return fieldName + " = \'" + value + "\'";
    }
}
