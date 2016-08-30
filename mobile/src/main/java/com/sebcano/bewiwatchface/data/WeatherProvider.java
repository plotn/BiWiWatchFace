package com.sebcano.bewiwatchface.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sebcano.bewiwatchface.data.WeatherContract.LocationEntry;
import com.sebcano.bewiwatchface.data.WeatherContract.WeatherEntry;

public class WeatherProvider extends ContentProvider {
    private static final String TAG = WeatherProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;
    static final int LOCATION_ITEM = 301;

    private static final SQLiteQueryBuilder sWeatherByLocationIdQueryBuilder;

    static{
        sWeatherByLocationIdQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationIdQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " + LocationEntry.TABLE_NAME +
                        " ON " + WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_LOC_KEY +
                        " = " + LocationEntry.TABLE_NAME + "." + LocationEntry._ID);
    }

    @Override
    public boolean onCreate() {
        Log.d( TAG, "onCreate: " );
        mOpenHelper = new WeatherDbHelper( getContext() );
        return true;
    }

    @Override
    public String getType( @NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE;
            case LOCATION_ITEM:
                return LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query( @NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case WEATHER:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder );
                break;
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocation(uri, projection, sortOrder);
                break;
            }
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder );
                break;
            }
            case LOCATION_ITEM: {
                retCursor = getLocationItem( uri, projection, sortOrder );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri( getContext().getContentResolver(), uri );
        return retCursor;
    }

    @Override
    public Uri insert( @NonNull Uri uri, ContentValues values) {
        Uri returnUri;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case WEATHER: {
                long _id = db.insert( WeatherEntry.TABLE_NAME, null, values );
                if ( _id > 0 ) {
                    returnUri = WeatherEntry.buildWeatherUri( _id );
                } else {
                    throw new android.database.SQLException( "Failed to insert row into " + uri );
                }
                break;
            }
            case LOCATION: {
                long _id = db.insert( LocationEntry.TABLE_NAME, null, values );
                if ( _id > 0 ) {
                    returnUri = LocationEntry.buildLocationUri( _id );
                } else {
                    throw new android.database.SQLException( "Failed to insert row into " + uri );
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete( @NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (selection == null || rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    public int update( @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        if ( selection == null) selection = "1";
        int rowsUpdated;
        switch (match) {
            case WEATHER:
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;
        uriMatcher.addURI( authority, WeatherContract.PATH_WEATHER, WEATHER );
        uriMatcher.addURI( authority, WeatherContract.PATH_WEATHER + "/#", WEATHER_WITH_LOCATION );
        uriMatcher.addURI( authority, WeatherContract.PATH_WEATHER + "/#/#", WEATHER_WITH_LOCATION_AND_DATE );
        uriMatcher.addURI( authority, WeatherContract.PATH_LOCATION, LOCATION );
        uriMatcher.addURI( authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ITEM );
        return uriMatcher;
    }

    //location._id = ?
    private static final String sLocationIdSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry._ID + " = ? ";


    private Cursor getWeatherByLocation(Uri uri, String[] projection, String sortOrder) {
        long locationId = WeatherEntry.getLocationIdFromUri(uri);
        String selection = sLocationIdSelection;
        String[] selectionArgs = new String[]{ Long.toString( locationId )};
        return sWeatherByLocationIdQueryBuilder.query( mOpenHelper.getReadableDatabase(),
                projection, selection, selectionArgs,
                null, null, sortOrder );
    }

    private Cursor getLocationItem(Uri uri, String[] projection, String sortOrder) {
        long locationId = LocationEntry.getLocationIdFromUri(uri);
        String selection = sLocationIdSelection;
        String[] selectionArgs = new String[]{ Long.toString( locationId )};
        return mOpenHelper.getReadableDatabase().query(
                LocationEntry.TABLE_NAME,
                projection, selection, selectionArgs,
                null, null, sortOrder );
    }

}
