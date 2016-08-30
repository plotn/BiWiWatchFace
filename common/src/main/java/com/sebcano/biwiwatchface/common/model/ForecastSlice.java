package com.sebcano.biwiwatchface.common.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class ForecastSlice {
    private static final String FIELD_START_UTC_MILLIS = "startUTCMillis";
    private static final String FIELD_END_LOCAL_HOUR = "endHour";
    private static final String FIELD_CONDITION_ID = "conditionId";
    private static final String FIELD_MIN_TEMP = "minTemp";
    private static final String FIELD_MAX_TEMP = "maxTemp";
    private static final String FIELD_FETCH_TS = "fetchTs";

    private long mUTCMillisStart;
    private int mLocalHourEnd;
    private int mConditionId;
    private float mMinTemp;
    private float mMaxTemp;
    private long mFetchTimestampUTCMillis;

    private long mNextSliceUTCSeconds;
    private boolean mHasValue;


    public static ForecastSlice buildCurrentSlice() {
        return buildSlice( System.currentTimeMillis() );
    }

    public ForecastSlice buildNextSlice() {
        return buildSlice( mNextSliceUTCSeconds * 1000 );
    }

    public boolean isInSlice( long utcDateSeconds ) {
        return utcDateSeconds < mNextSliceUTCSeconds;
    }

    public void merge( int conditionId, float minTemp, float maxTemp, long fetchTimestampUTCMillis ) {
        if ( !mHasValue ) {
            mConditionId = conditionId;
            mMinTemp = minTemp;
            mMaxTemp = maxTemp;
            mFetchTimestampUTCMillis = fetchTimestampUTCMillis;
            mHasValue = true;
        } else {
            mConditionId = Math.max( conditionId, mConditionId );
            mMinTemp = Math.min( minTemp, mMinTemp );
            mMaxTemp = Math.max( maxTemp, mMaxTemp );
            mFetchTimestampUTCMillis = Math.min( mFetchTimestampUTCMillis, fetchTimestampUTCMillis );
        }
    }

    public boolean hasValue() { return mHasValue; }

    public int getConditionId() {
        return mConditionId;
    }

    public float getMinTemp() {
        return mMinTemp;
    }

    public float getMaxTemp() {
        return mMaxTemp;
    }

    public long getUTCMillisStart() {
        return mUTCMillisStart;
    }

    public long getUTCMillisEnd() {
        return mNextSliceUTCSeconds*1000-1;
    }

    public int getLocalHourEnd() {
        return mLocalHourEnd;
    }

    public long getFetchTimestampUTCMillis() { return mFetchTimestampUTCMillis; }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject sliceJSON = new JSONObject();
        sliceJSON.put( FIELD_START_UTC_MILLIS, mUTCMillisStart );
        sliceJSON.put( FIELD_END_LOCAL_HOUR, mLocalHourEnd );
        sliceJSON.put( FIELD_CONDITION_ID, mConditionId );
        sliceJSON.put( FIELD_MIN_TEMP, mMinTemp );
        sliceJSON.put( FIELD_MAX_TEMP, mMaxTemp );
        sliceJSON.put( FIELD_FETCH_TS, mFetchTimestampUTCMillis );
        return sliceJSON;
    }

    public static ForecastSlice ofJSONObject( JSONObject json ) throws JSONException {
        long utcMillisStart = json.getLong( FIELD_START_UTC_MILLIS );
        ForecastSlice slice = buildSlice( utcMillisStart );
        slice.mLocalHourEnd = json.getInt( FIELD_END_LOCAL_HOUR );
        slice.mConditionId = json.getInt( FIELD_CONDITION_ID );
        slice.mMinTemp = (float) json.getDouble( FIELD_MIN_TEMP );
        slice.mMaxTemp = (float) json.getDouble( FIELD_MAX_TEMP );
        slice.mFetchTimestampUTCMillis = json.getLong( FIELD_FETCH_TS );
        return slice;
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
        newSlice.mUTCMillisStart = utcMillis;
        newSlice.mLocalHourEnd = calendar.get( Calendar.HOUR_OF_DAY );
        newSlice.mNextSliceUTCSeconds = calendar.getTimeInMillis() / 1000;
        return newSlice;
    }

    private ForecastSlice() {
    }
}
