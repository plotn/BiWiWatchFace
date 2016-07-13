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

import sca.biwiwatchface.data.Weather;

public class WeatherAdapter extends BaseAdapter {
    private static final String TAG = WeatherAdapter.class.getSimpleName();

    JSONArray mForecastArray;
    private static LayoutInflater inflater = null;

    WeatherAdapter( Context context, String jsonString ) {
        inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);

        try {
            JSONObject json = new JSONObject( jsonString );
            mForecastArray = json.getJSONArray( "weather" );

        } catch ( JSONException e ) {
            Log.e( TAG, "WeatherAdapter: ", e );
        }
    }

    @Override
    public int getCount() {
        return mForecastArray.length();
    }

    @Override
    public Object getItem( int position ) {
        try {
            return mForecastArray.get( position );
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
    public View getView( int position, View convertView, ViewGroup parent ) {
        View vi = convertView;
        if (convertView == null) vi = inflater.inflate( android.R.layout.simple_list_item_2, null);
        TextView text1 = (TextView) vi.findViewById(android.R.id.text1);
        TextView text2 = (TextView) vi.findViewById(android.R.id.text2);

        try {
            JSONObject forecast = (JSONObject) mForecastArray.get( position );
            String string1 = Weather.conditionIdToUnicode( forecast.getInt( "conditionId") )+ " "
                            + forecast.getString( "maxTemp" ) + "° | "
                            + forecast.getString( "minTemp" ) + "°";
            text1.setText( string1 );

            String string2 = forecast.get( "cityName" ) + " "
                    + forecast.getString( "startHour" ) + "h - "
                    + forecast.getString( "endHour" ) + "h";
            text2.setText( string2 );
        } catch ( JSONException e ) {
            Log.e( TAG, "WeatherAdapter: ", e );
        }


        return vi;
    }
}
