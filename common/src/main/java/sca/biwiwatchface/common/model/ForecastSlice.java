package sca.biwiwatchface.common.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class ForecastSlice {
    private static final String FIELD_START_UTC_MILLIS = "startUTCMillis";
    private static final String FIELD_END_LOCAL_HOUR = "endHour";
    private static final String FIELD_CONDITION_ID = "conditionId";
    private static final String FIELD_MIN_TEMP = "minTemp";
    private static final String FIELD_MAX_TEMP = "maxTemp";

    private int mLocalHourEnd;
    private long mUTCMillisStart;
    private long mNextSliceUTCSeconds;
    private boolean mHasValue;
    private int mConditionId;
    private float mMinTemp;
    private float mMaxTemp;

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
        if ( !mHasValue ) {
            mConditionId = conditionId;
            mMinTemp = minTemp;
            mMaxTemp = maxTemp;
            mHasValue = true;
        } else {
            mConditionId = Math.max( conditionId, mConditionId );
            mMinTemp = Math.min( minTemp, mMinTemp );
            mMaxTemp = Math.max( maxTemp, mMaxTemp );
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

    public int getLocalHourEnd() {
        return mLocalHourEnd;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject sliceJSON = new JSONObject();
        sliceJSON.put( FIELD_START_UTC_MILLIS, mUTCMillisStart );
        sliceJSON.put( FIELD_END_LOCAL_HOUR, mLocalHourEnd );
        sliceJSON.put( FIELD_CONDITION_ID, mConditionId );
        sliceJSON.put( FIELD_MIN_TEMP, mMinTemp );
        sliceJSON.put( FIELD_MAX_TEMP, mMaxTemp );
        return sliceJSON;
    }

    public static ForecastSlice ofJSONObject( JSONObject json ) throws JSONException {
        ForecastSlice slice = new ForecastSlice();
        slice.mUTCMillisStart = json.getLong( FIELD_START_UTC_MILLIS );
        slice.mLocalHourEnd = json.getInt( FIELD_END_LOCAL_HOUR );
        slice.mConditionId = json.getInt( FIELD_CONDITION_ID );
        slice.mMinTemp = (float) json.getDouble( FIELD_MIN_TEMP );
        slice.mMaxTemp = (float) json.getDouble( FIELD_MAX_TEMP );
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
