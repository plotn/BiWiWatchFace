package sca.biwiwatchface.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class ForecastSlice {
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
        return buildSlice( mNextSliceUTCSeconds * 1000 );
    }

    public boolean isInSlice( long utcDateSeconds ) {
        return utcDateSeconds < mNextSliceUTCSeconds;
    }

    public void merge( int conditionId, float minTemp, float maxTemp ) {
        if ( !hasValue ) {
            mConditionId = conditionId;
            mMinTemp = minTemp;
            mMaxTemp = maxTemp;
            hasValue = true;
        } else {
            mConditionId = Math.max( conditionId, mConditionId );
            mMinTemp = Math.min( minTemp, mMinTemp );
            mMaxTemp = Math.max( maxTemp, mMaxTemp );
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
        if ( localHour >= 20 ) {
            calendar.add( Calendar.DAY_OF_YEAR, 1 );
            calendar.set( Calendar.HOUR_OF_DAY, 8 );
        } else {
            int newLocalHour = 8;
            if ( localHour >= 8 && localHour < 14 ) {
                newLocalHour = 14;
            } else if ( localHour >= 14 && localHour < 20 ) {
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

    private ForecastSlice() {
    }
}
