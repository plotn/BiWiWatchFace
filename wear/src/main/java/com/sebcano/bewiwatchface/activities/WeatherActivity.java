package com.sebcano.bewiwatchface.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.ListView;

import com.sebcano.bewiwatchface.R;

public class WeatherActivity extends Activity {

    //private static final String TST_JSON = "{\"weather\":[{\"startHour\":0,\"endHour\":8,\"conditionId\":500,\"minTemp\":8,\"maxTemp\":11,\"cityName\":\"Gieres\"},{\"startHour\":8,\"endHour\":14,\"conditionId\":500,\"minTemp\":9,\"maxTemp\":13,\"cityName\":\"Gieres\"},{\"startHour\":14,\"endHour\":20,\"conditionId\":500,\"minTemp\":11,\"maxTemp\":14,\"cityName\":\"Gieres\"},{\"startHour\":20,\"endHour\":8,\"conditionId\":800,\"minTemp\":8,\"maxTemp\":12,\"cityName\":\"Gieres\"},{\"startHour\":8,\"endHour\":14,\"conditionId\":800,\"minTemp\":9,\"maxTemp\":15,\"cityName\":\"Gieres\"},{\"startHour\":14,\"endHour\":20,\"conditionId\":800,\"minTemp\":16,\"maxTemp\":18,\"cityName\":\"Gieres\"},{\"startHour\":20,\"endHour\":8,\"conditionId\":800,\"minTemp\":5,\"maxTemp\":16,\"cityName\":\"Gieres\"},{\"startHour\":8,\"endHour\":14,\"conditionId\":800,\"minTemp\":9,\"maxTemp\":17,\"cityName\":\"Gieres\"},{\"startHour\":14,\"endHour\":20,\"conditionId\":800,\"minTemp\":21,\"maxTemp\":22,\"cityName\":\"Gieres\"},{\"startHour\":20,\"endHour\":8,\"conditionId\":800,\"minTemp\":13,\"maxTemp\":20,\"cityName\":\"Gieres\"}],\"random\":0.40572921799090156}";

    public static final String INTENT_EXTRA_WEATHER_JSON = "weatherJson";

    private ListView mListView;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_weather );
        final WatchViewStub stub = (WatchViewStub) findViewById( R.id.watch_view_stub );
        stub.setOnLayoutInflatedListener( new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated( WatchViewStub stub ) {
                Intent intent = getIntent();
                String json = intent.getStringExtra( INTENT_EXTRA_WEATHER_JSON );

                mListView = (ListView) stub.findViewById( R.id.weatherList );
                mListView.setAdapter( new WeatherAdapter( WeatherActivity.this, json ) );
            }
        } );
    }
}
