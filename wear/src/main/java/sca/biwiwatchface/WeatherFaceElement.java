package sca.biwiwatchface;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.wearable.watchface.WatchFaceService;
import android.util.Log;
import android.view.WindowInsets;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import sca.biwiwatchface.activities.WeatherActivity;
import sca.biwiwatchface.data.Weather;

public class WeatherFaceElement extends AbstractFaceElement {

    private static final String TAG = WeatherFaceElement.class.getSimpleName();

    private Paint mWeatherPaint;

    private String mWeatherJsonString = "{}";
    private String mDisplayedString = "";
    private int mLastYDraw;

    public WeatherFaceElement( Context context) {
        super( context );
        int textColor = getColor(R.color.date);
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
    public void onApplyWindowInsets( WindowInsets insets ) {
        float textSize = getDimension( R.dimen.digital_calendar_size );
        mWeatherPaint.setTextSize(textSize);
    }

    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {
        canvas.drawText( mDisplayedString, x, y, mWeatherPaint );
        mLastYDraw = y;
    }

    public void setDataItem( DataItem item ) {
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

        mDisplayedString = "";

        mWeatherJsonString = dataMap.getString( "json" );
        Log.d( TAG, "setDataItem: " + mWeatherJsonString );

        try {
            JSONObject json = new JSONObject( mWeatherJsonString );
            JSONArray forecastArray = json.getJSONArray( "weather" );
            if (forecastArray.length() > 0 ) {
                JSONObject forecast = forecastArray.getJSONObject( 0 );
                int conditionId = forecast.getInt( "conditionId");
                mDisplayedString +=
                        Weather.conditionIdToUnicode( conditionId )+ " "
                        + forecast.getString( "maxTemp" ) + "° | "
                        + forecast.getString( "minTemp" ) + "°"
                ;
            }
        } catch ( JSONException e ) {
            Log.e( TAG, "setDataItem: ", e );
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
