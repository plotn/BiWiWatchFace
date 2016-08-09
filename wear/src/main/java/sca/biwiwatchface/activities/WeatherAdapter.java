package sca.biwiwatchface.activities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import sca.biwiwatchface.R;
import sca.biwiwatchface.common.model.ForecastSlice;
import sca.biwiwatchface.data.Weather;

public class WeatherAdapter extends BaseAdapter {
    private static final String TAG = WeatherAdapter.class.getSimpleName();

    private final static int VIEW_TYPE_HEADER = 0;
    private final static int VIEW_TYPE_FORECAST = 1;
    private final static int VIEW_TYPE_FOOTER = 2;

    JSONArray mForecastArray;
    JSONObject mLocationJson;
    private static LayoutInflater mInflater = null;

    private DateFormat mDateFormat = new SimpleDateFormat("EEE H", Locale.getDefault());
    private Context mContext;

    WeatherAdapter( Context context, String jsonString ) {
        mContext = context;

        mInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);

        try {
            JSONObject json = new JSONObject( jsonString );
            mForecastArray = json.getJSONArray( "weather" );
            mLocationJson = json.getJSONObject( "location" );

        } catch ( JSONException e ) {
            Log.e( TAG, "WeatherAdapter: ", e );
        }
    }

    @Override
    public int getCount() {
        return mForecastArray.length() +2;
    }

    @Override
    public Object getItem( int position ) {
        try {
            if (position == 0) {
                return mLocationJson;
            }
            return mForecastArray.get( position-1 );
        } catch ( JSONException e ) {
            Log.e( TAG, "WeatherAdapter: ", e );
        }
        return null;
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType( int position ) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if ( position == getCount()-1 ) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_FORECAST;
        }
    }


    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        int type = getItemViewType( position );
        View vi = convertView;

        try {
            if (VIEW_TYPE_FORECAST == type) {
                if ( convertView == null )
                    vi = mInflater.inflate( R.layout.list_item_forecast, null );

                TextView textCondition = (TextView) vi.findViewById( R.id.list_item_forecast_condition );
                TextView textMaxTemp = (TextView) vi.findViewById( R.id.list_item_forecast_max_temperature );
                TextView textMinTemp = (TextView) vi.findViewById( R.id.list_item_forecast_min_temperature );
                TextView textDate = (TextView) vi.findViewById( R.id.list_item_forecast_date );

                JSONObject forecast = mForecastArray.getJSONObject( position - 1 );
                ForecastSlice slice = ForecastSlice.ofJSONObject( forecast );
                textCondition.setText( Weather.conditionIdToUnicode( slice.getConditionId() ) );
                textMaxTemp.setText( Math.round( slice.getMaxTemp() ) + "°" );
                textMinTemp.setText( Math.round( slice.getMinTemp() ) + "°" );

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis( slice.getUTCMillisStart() );
                String szStartDate = mDateFormat.format( calendar.getTime() );

                long now = System.currentTimeMillis();
                String ageWarning = "";
                final int MILLIS_IN_HOUR = 60 * 60 * 1000;
                long fetchTs = slice.getFetchTimestampUTCMillis();
                long ageInHour = (now - fetchTs) / MILLIS_IN_HOUR;
                if ( ageInHour > 3 ) {
                    ageWarning = " (" + String.format( mContext.getString( R.string.format_weather_age_warning ), ageInHour ) + ")";
                }

                String string2 = String.format( "%sh - %sh%s", szStartDate, slice.getLocalHourEnd(), ageWarning );
                textDate.setText( string2 );

            } else if (type == VIEW_TYPE_HEADER) {
                if (convertView == null) vi = mInflater.inflate( R.layout.list_item_header, null);

                TextView textCityName = (TextView) vi.findViewById( R.id.list_header_city_name );

                textCityName.setText( mLocationJson.getString( "cityName" ) );

            } else {
                if (convertView == null) vi = mInflater.inflate( R.layout.list_item_footer, null);

                TextView textCoordinates = (TextView) vi.findViewById( R.id.list_header_coordinates );

                textCoordinates.setText( String.format( "Lat: %s Lon: %s",
                        mLocationJson.getDouble( "askedLat" ), mLocationJson.getDouble( "askedLon" ) ) );

            }
        } catch ( JSONException e ) {
            Log.e( TAG, "WeatherAdapter: ", e );
        }

        return vi;
    }
}
