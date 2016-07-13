package sca.biwiwatchface.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import sca.biwiwatchface.data.WeatherContract.WeatherEntry;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
public class WeatherProvider extends ContentProvider {
    private static final String TAG = WeatherProvider.class.getSimpleName();

    WeatherDbHelper mOpenHelper;

    /*
     * Always return true, indicating that the
     * provider loaded correctly.
     */
    @Override
    public boolean onCreate() {
        Log.d( TAG, "onCreate: " );
        mOpenHelper = new WeatherDbHelper( getContext() );
        return true;
    }

    /*
     * Return no type for MIME type
     */
    @Override
    public String getType(Uri uri) {
        Log.d( TAG, "getType: " );
        return null;
    }

    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mOpenHelper.getReadableDatabase().query(
                WeatherEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri returnUri = null;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long _id = db.insert( WeatherEntry.TABLE_NAME, null, values );
        if ( _id > 0 ) {
            returnUri = WeatherEntry.buildWeatherUri(_id);
        } else {
            throw new android.database.SQLException("Failed to insert row into " + uri);
        }
        //Log.d( TAG, "insert: " + returnUri );
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        return db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
    }

    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d( TAG, "update: " );
        return 0;
    }
}
