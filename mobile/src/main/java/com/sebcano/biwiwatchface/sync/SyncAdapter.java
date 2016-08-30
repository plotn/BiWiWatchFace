package com.sebcano.biwiwatchface.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.sebcano.biwiwatchface.AppInit;
import com.sebcano.biwiwatchface.BuildConfig;
import com.sebcano.biwiwatchface.common.model.ForecastLocation;
import com.sebcano.biwiwatchface.common.model.ForecastSlice;
import com.sebcano.biwiwatchface.data.WeatherContract.LocationEntry;
import com.sebcano.biwiwatchface.data.WeatherContract.WeatherEntry;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SBWWF SyncAdapter";


    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter( Context context, boolean autoInitialize ) {
        super( context, autoInitialize );
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs ) {
        super( context, autoInitialize, allowParallelSyncs );
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync( Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult ) {
        AppInit.onStartup(getContext());
        Log.d( TAG, "onPerformSync" );
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder( getContext() )
                .addApi( LocationServices.API )
                .addApi( Wearable.API )
                .build();
        ConnectionResult connectionResult = googleApiClient.blockingConnect();
        if (connectionResult.isSuccess()) {
            LocationProvider locationProvider = new LocationProvider();
            Location location = locationProvider.getLocation( googleApiClient );
            long locId = fetchWeather( location );
            sendWeatherToWatch(locId, location, googleApiClient);
        }

        googleApiClient.disconnect();
    }

    private long fetchWeather( Location location ) {
        String weatherJson = getWeatherJson( location );
        return parseWeatherJson( weatherJson );
    }

    private String getWeatherJson( Location location ) {
        String resJson = null;

        final int NUM_REPORTS = 3 /* days */ * 24 /* h */ / 3 /* h/report */;

        Uri builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/forecast?").buildUpon()
                .appendQueryParameter( "lat", Double.toString( location.getLatitude() ) )
                .appendQueryParameter( "lon", Double.toString( location.getLongitude() ) )
                .appendQueryParameter( "units", "metric" )
                .appendQueryParameter( "cnt", Integer.toString( NUM_REPORTS ) )
                .appendQueryParameter( "appid", BuildConfig.OPEN_WEATHER_MAP_API_KEY )
                .build();
        Log.d( TAG, "fetchWeather: " + builtUri.toString().replace( BuildConfig.OPEN_WEATHER_MAP_API_KEY, "****" ) );

        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(builtUri.toString());
            urlConnection= (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resJson = buffer.toString();
            } catch ( Exception e ) {
                Log.e( TAG, "fetchWeather: ", e  );
            }

        } catch ( Exception e ) {
            Log.e( TAG, "fetchWeather: ", e  );
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        Log.d( TAG, "getWeatherJson: resJons=" + resJson);
        return resJson;
    }

    private long parseWeatherJson( String jsonString ) {
        long cityId = -1;
        try {
            JSONObject json = new JSONObject(jsonString);

            JSONObject cityJson = json.getJSONObject("city");
            cityId = cityJson.getLong( "id" );
            String cityName = cityJson.getString( "name" );
            JSONObject cityCoord = cityJson.getJSONObject( "coord" );
            double cityLon = cityCoord.getDouble( "lon" );
            double cityLat = cityCoord.getDouble( "lat" );
            addLocation( cityId, cityName, cityLat, cityLon );

            JSONArray forecastJsonArray = json.getJSONArray( "list" );
            Log.d( TAG, "parseWeatherJson: jsonarray.length=" + forecastJsonArray.length() );
            ContentValues tbCVForecast[] = new ContentValues[forecastJsonArray.length()];
            long now = System.currentTimeMillis();
            for (int i = 0; i<forecastJsonArray.length(); i++) {
                JSONObject forecastJson = forecastJsonArray.getJSONObject( i );
                JSONObject weatherJson = forecastJson.getJSONArray( "weather" ).getJSONObject( 0 );
                JSONObject mainJson = forecastJson.getJSONObject( "main" );

                ContentValues cvForecast = new ContentValues();
                cvForecast.put( WeatherEntry.COLUMN_DATE, forecastJson.getLong( "dt" ) );
                cvForecast.put( WeatherEntry.COLUMN_WEATHER_ID, weatherJson.getInt( "id" ) );
                cvForecast.put( WeatherEntry.COLUMN_TEMP, mainJson.getInt( "temp" ) );
                cvForecast.put( WeatherEntry.COLUMN_MIN_TEMP, mainJson.getInt( "temp_min" ) );
                cvForecast.put( WeatherEntry.COLUMN_MAX_TEMP, mainJson.getInt( "temp_max" ) );
                cvForecast.put( WeatherEntry.COLUMN_LOC_KEY, cityId );
                cvForecast.put( WeatherEntry.COLUMN_FETCH_TS, now );
                tbCVForecast[i] = cvForecast;
            }

            mContentResolver.bulkInsert( WeatherEntry.CONTENT_URI, tbCVForecast );

            String nowSeconds = Long.toString( System.currentTimeMillis() / 1000 );
            mContentResolver.delete( WeatherEntry.CONTENT_URI, WeatherEntry.COLUMN_DATE + "<= ?", new String[]{nowSeconds} );
        } catch ( JSONException e ) {
            Log.e( TAG, "parseWeatherJson: ", e );
        }
        return cityId;
    }

    private void addLocation(long cityId, String cityName, double lat, double lon) {
        ContentValues locValues = new ContentValues();
        locValues.put( LocationEntry._ID, cityId);
        locValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
        locValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
        locValues.put(LocationEntry.COLUMN_COORD_LONG, lon);
        mContentResolver.insert(LocationEntry.CONTENT_URI, locValues);
    }


    private void sendWeatherToWatch( long locationId, Location location, GoogleApiClient googleApiClient ) {
        Uri weatherAtLocation = WeatherEntry.buildWeatherLocation( locationId );
        Cursor weatherCursor = mContentResolver.query( weatherAtLocation, null, null, null, null );

        if (weatherCursor != null) {
            Log.d( TAG, "sendWeatherToWatch: cursor.getCount()=" + weatherCursor.getCount());
            if (weatherCursor.getCount() > 0) {
                final int idxDate = weatherCursor.getColumnIndex( WeatherEntry.COLUMN_DATE );
                final int idxConditionId = weatherCursor.getColumnIndex( WeatherEntry.COLUMN_WEATHER_ID );
                final int idxMinTemp = weatherCursor.getColumnIndex( WeatherEntry.COLUMN_MIN_TEMP );
                final int idxMaxTemp = weatherCursor.getColumnIndex( WeatherEntry.COLUMN_MAX_TEMP );
                final int idxFetchTs = weatherCursor.getColumnIndex( WeatherEntry.COLUMN_FETCH_TS );

                ForecastSlice currentSlice = ForecastSlice.buildCurrentSlice();
                ArrayList<ForecastSlice> lstSlices = new ArrayList<>( 6 );


                while ( weatherCursor.moveToNext() ) {
                    long dateSeconds = weatherCursor.getLong( idxDate );
                    if ( !currentSlice.isInSlice( dateSeconds ) ) {
                        if ( currentSlice.hasValue() ) {
                            lstSlices.add( currentSlice );
                        }
                        currentSlice = currentSlice.buildNextSlice();
                    }
                    currentSlice.merge(
                            weatherCursor.getInt( idxConditionId ),
                            weatherCursor.getFloat( idxMinTemp ),
                            weatherCursor.getFloat( idxMaxTemp ),
                            weatherCursor.getLong( idxFetchTs ) );
                }
                if ( currentSlice.hasValue() ) {
                    lstSlices.add( currentSlice );
                }
                weatherCursor.close();

                ForecastLocation forecastLocation = new ForecastLocation( location );
                Uri locationUri = LocationEntry.buildLocationUri( locationId );
                Cursor locationCursor = mContentResolver.query( locationUri, null, null, null, null );
                if ( locationCursor != null ) {
                    if ( locationCursor.moveToFirst() ) {
                        final int idxName = locationCursor.getColumnIndex( LocationEntry.COLUMN_CITY_NAME );
                        forecastLocation.merge( locationCursor.getString( idxName ) );
                    }
                    locationCursor.close();
                }

                try {
                    JSONArray forecastJsonArray = new JSONArray();
                    for ( ForecastSlice slice : lstSlices ) {
                        forecastJsonArray.put( slice.toJSONObject() );
                    }
                    JSONObject sentJson = new JSONObject();
                    sentJson.put( "weather", forecastJsonArray );
                    sentJson.put( "location", forecastLocation.toJSONObject() );
                    Log.d( TAG, "sendWeatherToWatch: json" + sentJson.toString() );

                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create( "/weather" );
                    putDataMapRequest.getDataMap().putString( "json", sentJson.toString() );
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();
                    request.setUrgent();
                    Wearable.DataApi.putDataItem( googleApiClient, request );

                } catch ( JSONException e ) {
                    Log.e( TAG, "sendWeatherToWatch: ", e );
                }
            }

        }

    }

}
