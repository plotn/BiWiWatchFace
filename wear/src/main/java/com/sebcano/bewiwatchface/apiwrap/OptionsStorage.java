package com.sebcano.bewiwatchface.apiwrap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OptionsStorage {

    private Context mContext;
    private SharedPreferences mPrefs;

    public OptionsStorage( Context context ) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences( context );
    }

    public void putString( int keyId, String value ) {
        mPrefs.edit()
                .putString( mContext.getString( keyId ), value )
                .apply();
    }

    public String getString( int keyId, int defaultValueId ) {
        return mPrefs.getString( mContext.getString( keyId ), mContext.getString( defaultValueId ) );
    }

}
