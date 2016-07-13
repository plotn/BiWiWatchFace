package sca.biwiwatchface.sync;

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
import java.util.Calendar;

import sca.biwiwatchface.AppInit;
import sca.biwiwatchface.data.WeatherContract.WeatherEntry;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();


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
        Log.d( TAG, "onPerformSync" );

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder( getContext() )
                .addApi( LocationServices.API )
                .addApi( Wearable.API )
                .build();
        ConnectionResult connectionResult = googleApiClient.blockingConnect();
        if (connectionResult.isSuccess()) {

            Location location = getLocation( googleApiClient );
            getWeather( location );
            sendWeatherToWatch(googleApiClient);

        }

        googleApiClient.disconnect();
    }

    private Location getLocation( GoogleApiClient googleApiClient ) {
        Location resLocation = null;
        try {
            AppInit.configureLocationApi( googleApiClient );
            resLocation = LocationServices.FusedLocationApi.getLastLocation( googleApiClient );
            Log.d( TAG, "getLastLocation: " + resLocation );
        } catch (SecurityException e) {
            Log.w( TAG, "onConnected: ", e );
        }
        return resLocation;
    }

    private void getWeather( Location location ) {
        String weatherJson = getWeatherJson( location );
        parseWeatherJson( weatherJson );
    }

    private String getWeatherJson( Location location ) {
        String resJson = null;

        final int NUM_REPORTS = 3 /* days */ * 24 /* h */ / 3 /* h/report */;

        Uri builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/forecast?").buildUpon()
                .appendQueryParameter( "lat", Double.toString( location.getLatitude() ) )
                .appendQueryParameter( "lon", Double.toString( location.getLongitude() ) )
                .appendQueryParameter( "units", "metric" )
                .appendQueryParameter( "cnt", Integer.toString( NUM_REPORTS ) )
                .appendQueryParameter( "appid", "bfd9ed7936c70546a34990813b7f085e" )
                .build();
        Log.d( TAG, "getWeather: " + builtUri );

        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(builtUri.toString());
            urlConnection= (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuffer buffer = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resJson = buffer.toString();
            } catch ( Exception e ) {
                Log.e( TAG, "getWeather: ", e  );
            }

        } catch ( Exception e ) {
            Log.e( TAG, "getWeather: ", e  );
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }

        return resJson;
    }

    private void parseWeatherJson( String jsonString ) {
        try {
            JSONObject json = new JSONObject(jsonString);

            JSONObject cityJson = json.getJSONObject("city");
            String cityName = cityJson.getString( "name" );

            JSONArray forecastJsonArray = json.getJSONArray( "list" );
            ContentValues tbCVForecast[] = new ContentValues[forecastJsonArray.length()];
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
                cvForecast.put( WeatherEntry.COLUMN_CITY_NAME, cityName );
                tbCVForecast[i] = cvForecast;
            }

            ContentResolver resolver = getContext().getContentResolver();
            int numRowCreated = resolver.bulkInsert( WeatherEntry.CONTENT_URI, tbCVForecast );

            String nowSeconds = Long.toString( System.currentTimeMillis() / 1000 );
            int numRowDeleted = resolver.delete( WeatherEntry.CONTENT_URI, WeatherEntry.COLUMN_DATE + "<= ?", new String[]{nowSeconds} );

        } catch ( JSONException e ) {
            Log.e( TAG, "parseWeatherJson: ", e );
        }
    }

    private static class ForecastSlice {
        private int mLocalHourEnd;
        private long mUTCMillisStart;
        private long mNextSliceUTCSeconds;
        private boolean hasValue;
        private int mConditionId;
        private float mMinTemp;
        private float mMaxTemp;
        private String mCityName;

        public static ForecastSlice buildCurrentSlice() {
            return buildSlice( System.currentTimeMillis() );
        }

        public ForecastSlice buildNextSlice() {
            return buildSlice( mNextSliceUTCSeconds*1000 );
        }

        public boolean isInSlice( long utcDateSeconds ) {
            return utcDateSeconds < mNextSliceUTCSeconds;
        }

        public void merge( int conditionId, float minTemp, float maxTemp, String cityName ) {
            if (!hasValue) {
                mConditionId = conditionId;
                mMinTemp = minTemp;
                mMaxTemp = maxTemp;
                mCityName = cityName;
                hasValue = true;
            } else {
                mConditionId = Math.max( conditionId, mConditionId );
                mMinTemp = Math.min( minTemp, mMinTemp );
                mMaxTemp = Math.max( maxTemp, mMaxTemp );
                if (! mCityName.contains( cityName )) {
                    mCityName += ", " + cityName;
                }
            }
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject sliceJSON = new JSONObject();
            sliceJSON.put( "startUTCMillis", mUTCMillisStart );
            sliceJSON.put( "endHour", mLocalHourEnd );
            sliceJSON.put( "conditionId", mConditionId );
            sliceJSON.put( "minTemp", mMinTemp );
            sliceJSON.put( "maxTemp", mMaxTemp );
            sliceJSON.put( "cityName", mCityName );
            return sliceJSON;
        }

        @Override
        public String toString() {
            return "[->" + mLocalHourEnd + "] " + mConditionId
                    + "[" + mMinTemp + "°-" + mMaxTemp + "°]";
        }

        private static ForecastSlice buildSlice( long utcMillis ) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis( utcMillis );
            int localHour = calendar.get( Calendar.HOUR_OF_DAY );
            if (localHour >= 20 ) {
                calendar.add( Calendar.DAY_OF_YEAR, 1 );
                calendar.set( Calendar.HOUR_OF_DAY, 8 );
            } else {
                int newLocalHour = 8;
                if (localHour >= 8 && localHour < 14) {
                    newLocalHour = 14;
                } else if (localHour >= 14 && localHour < 20) {
                    newLocalHour = 20;
                }
                calendar.set( Calendar.HOUR_OF_DAY, newLocalHour );
            }
            calendar.set( Calendar.MINUTE, 0 );
            calendar.set( Calendar.SECOND, 0 );

            ForecastSlice newSlice = new ForecastSlice();
            newSlice.mLocalHourEnd = calendar.get( Calendar.HOUR_OF_DAY );
            newSlice.mUTCMillisStart = utcMillis;
            newSlice.mNextSliceUTCSeconds = calendar.getTimeInMillis() / 1000;
            return newSlice;
        }

        private ForecastSlice() {}
    }


    private void sendWeatherToWatch( GoogleApiClient googleApiClient ) {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = resolver.query( WeatherEntry.CONTENT_URI, null, null, null, null );

        if (cursor != null) {
            final int idxDate = cursor.getColumnIndex( WeatherEntry.COLUMN_DATE );
            final int idxConditionId = cursor.getColumnIndex( WeatherEntry.COLUMN_WEATHER_ID );
            final int idxMinTemp = cursor.getColumnIndex( WeatherEntry.COLUMN_MIN_TEMP );
            final int idxMaxTemp = cursor.getColumnIndex( WeatherEntry.COLUMN_MAX_TEMP );
            final int idxCityName = cursor.getColumnIndex( WeatherEntry.COLUMN_CITY_NAME );

            ForecastSlice currentSlice = ForecastSlice.buildCurrentSlice();
            ArrayList<ForecastSlice> lstSlices = new ArrayList<>( 6 );
            lstSlices.add( currentSlice );

            while ( cursor.moveToNext() ) {
                long dateSeconds = cursor.getLong( idxDate );
                if ( !currentSlice.isInSlice( dateSeconds ) ) {
                    currentSlice = currentSlice.buildNextSlice();
                    lstSlices.add( currentSlice );
                }
                currentSlice.merge(
                        cursor.getInt( idxConditionId ),
                        cursor.getFloat( idxMinTemp ),
                        cursor.getFloat( idxMaxTemp ),
                        cursor.getString( idxCityName ));
            }
            cursor.close();

            try {
                JSONArray forecastJsonArray = new JSONArray();
                for ( ForecastSlice slice : lstSlices ) {
                    forecastJsonArray.put( slice.toJSONObject() );
                }
                JSONObject sentJson = new JSONObject();
                sentJson.put( "weather", forecastJsonArray );
                sentJson.put( "random", Math.random() );
                Log.d( TAG, "sendWeatherToWatch: json" + sentJson.toString() );

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create( "/weather" );
                putDataMapRequest.getDataMap().putString( "json", sentJson.toString() );
                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                request.setUrgent();
                Wearable.DataApi.putDataItem(googleApiClient, request);

            } catch ( JSONException e ) {
                Log.e( TAG, "sendWeatherToWatch: ", e );
            }

        }

    }

}
