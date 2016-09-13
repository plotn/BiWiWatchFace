package com.sebcano.bewiwatchface;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.wearable.watchface.WatchFaceService;
import android.util.Log;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.sebcano.bewiwatchface.activities.WeatherActivity;
import com.sebcano.bewiwatchface.apiwrap.OptionsStorage;
import com.sebcano.bewiwatchface.common.model.ForecastSlice;
import com.sebcano.bewiwatchface.common.model.TemperatureUnit;
import com.sebcano.bewiwatchface.data.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class WeatherFaceElement extends AbstractFaceElement {

    private static final String TAG = WeatherFaceElement.class.getSimpleName();

    private Settings mSettings;

    private Paint mWeatherPaint;

    private String mWeatherJsonString = null;
    private String mDisplayedString = "";
    private long mNextSliceUTCMillis;
    private int mLastYDraw;

    public WeatherFaceElement( Context context) {
        super( context );
        mSettings = new Settings( new OptionsStorage( getContext() ) );

        int textColor = getColor(R.color.weather);
        Typeface typefaceMedium = Typeface.create("sans-serif", Typeface.NORMAL);

        mWeatherPaint = new Paint();
        mWeatherPaint.setColor(textColor);
        mWeatherPaint.setTypeface(typefaceMedium);
        mWeatherPaint.setTextAlign(Paint.Align.CENTER);

        setAntiAlias(true);
    }

    @Override
    public void setAntiAlias( boolean enabled ) {
        mWeatherPaint.setAntiAlias( true );
    }

    @Override
    public void setTextSize( float textSize ) {
        mWeatherPaint.setTextSize(textSize);
    }

    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {
        long nowUTCMillis = calendar.getTimeInMillis();
        if (mNextSliceUTCMillis > 0 && nowUTCMillis >= mNextSliceUTCMillis) {
            cacheDisplayedWeather( nowUTCMillis );
        }
        canvas.drawText( mDisplayedString, x, y, mWeatherPaint );
        mLastYDraw = y;
    }

    public void setDataItem( DataItem item ) {
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

        mDisplayedString = "";

        mWeatherJsonString = dataMap.getString( "json" );
        Log.d( TAG, "setDataItem: " + mWeatherJsonString );

        cacheDisplayedWeather( System.currentTimeMillis() );
        postInvalidate();
    }

    public void cacheDisplayedWeather( long nowUTCMillis ) {
        mDisplayedString = "";
        if (mWeatherJsonString != null ) {
            try {
                JSONObject json = new JSONObject( mWeatherJsonString );
                JSONArray forecastArray = json.getJSONArray( "weather" );
                if ( forecastArray.length() > 0 ) {
                    JSONObject forecast = forecastArray.getJSONObject( 0 );
                    ForecastSlice nowSlice = ForecastSlice.ofJSONObject( forecast );

                    for ( int i = 1; i < forecastArray.length(); i++ ) {
                        forecast = forecastArray.getJSONObject( i );
                        ForecastSlice slice = ForecastSlice.ofJSONObject( forecast );
                        long sliceStartUTCMillis = slice.getUTCMillisStart();
                        if ( sliceStartUTCMillis <= nowUTCMillis ) {
                            nowSlice = slice;
                        } else {
                            mNextSliceUTCMillis = sliceStartUTCMillis;
                            break;
                        }
                    }

                    TemperatureUnit unit = mSettings.getTemperatureUnit();
                    float maxTemp = nowSlice.getMaxTemp(unit);
                    float minTemp = nowSlice.getMinTemp(unit);

                    mDisplayedString = String.format( "%s %s° | %s°",
                            Weather.conditionIdToUnicode( nowSlice.getConditionId() ),
                            Math.round(maxTemp),
                            Math.round(minTemp) );
                }
            } catch ( JSONException e ) {
                Log.e( TAG, "setDataItem: ", e );
            }
        }
    }

    public void onTapCommand(int tapType, int x, int y, long eventTime) {
        if ( tapType == WatchFaceService.TAP_TYPE_TAP && y < mLastYDraw ) {
            Intent weatherIntent = new Intent( getContext(), WeatherActivity.class );
            weatherIntent.putExtra( WeatherActivity.INTENT_EXTRA_WEATHER_JSON, mWeatherJsonString );
            startActivity(weatherIntent);
        }
    }
}
