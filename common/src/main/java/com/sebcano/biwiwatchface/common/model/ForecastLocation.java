package com.sebcano.biwiwatchface.common.model;


import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

public class ForecastLocation {
    private Location mAskedLocation;
    private String mCityName;

    public ForecastLocation( Location askedLocation ) {
        mAskedLocation = askedLocation;
    }

    public void merge( String cityName ) {
        if (null == mCityName) {
            mCityName = cityName;
        } else if ( ! mCityName.contains( cityName )) {
            mCityName += ", " + cityName;
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject locationJSON = new JSONObject();
        locationJSON.put( "askedLat", mAskedLocation.getLatitude() );
        locationJSON.put( "askedLon", mAskedLocation.getLongitude() );
        locationJSON.put( "cityName", mCityName );
        return locationJSON;
    }
}