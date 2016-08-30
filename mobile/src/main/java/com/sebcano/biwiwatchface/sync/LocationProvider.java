package com.sebcano.biwiwatchface.sync;

import android.location.Location;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.atomic.AtomicReference;

import com.sebcano.biwiwatchface.AppInit;

public class LocationProvider implements LocationListener {

    private static final String TAG = "SBWWF LocationProvider";
    private static final double MAX_AGE_LAST_LOCATION_MILLIS = 60*60*1000;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationRequest mLocationRequest;
    private AtomicReference<Location> mRequestLocation;

    public Location getLocation( GoogleApiClient googleApiClient) {
        try {
            AppInit.configureLocationApi( googleApiClient );
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation( googleApiClient );
            Log.d( TAG, "getLocation: lastLocation=" + lastLocation );
            boolean validLastLocation = ( lastLocation != null
                    && ( (System.currentTimeMillis()-lastLocation.getTime()) < MAX_AGE_LAST_LOCATION_MILLIS) );
            Log.d( TAG, "getLocation: validLastLocation=" + validLastLocation );
            if (validLastLocation) {
                return lastLocation;
            } else {
                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
                mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

                // Simulate a blocking requestLocationUpdates
                mRequestLocation = new AtomicReference<>();
                HandlerThread handlerThread = new HandlerThread("requestLocationUpdates handler");
                handlerThread.start();
                Looper looper = handlerThread.getLooper();
                LocationServices.FusedLocationApi.requestLocationUpdates( googleApiClient, mLocationRequest, this, looper );
                int i = 60;
                while (i-->0 && mRequestLocation.get() == null) {
                    Log.d( TAG, "getLocation: sleeping " + i );
                    Thread.sleep( 1000 );
                }
                handlerThread.quitSafely();
                LocationServices.FusedLocationApi.removeLocationUpdates( googleApiClient, this );
                Log.d( TAG, "getLocation: mRequestLocation=" + mRequestLocation.get() );
                Location requestLocation = mRequestLocation.get();
                Location resLocation = requestLocation != null ? requestLocation : lastLocation;
                Log.d( TAG, "getLocation: returning=" + resLocation );
                return resLocation;
            }
        } catch (InterruptedException | SecurityException e) {
            Log.w( TAG, "getLocation: ", e );
        }
        return null;
    }

    @Override
    public void onLocationChanged( Location location ) {
        Log.d( TAG, "onLocationChanged: " + location );
        mRequestLocation.set(location);
    }
}
