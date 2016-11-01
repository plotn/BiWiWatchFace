package com.sebcano.bewiwatchface.sync;

import android.location.Location;
import android.os.HandlerThread;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.sebcano.bewiwatchface.AppInit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class LocationProvider implements LocationListener {

    static Logger log = LoggerFactory.getLogger("SBWWF LocationProvider");

    private static final double MAX_AGE_LAST_LOCATION_MILLIS = 60*60*1000;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationRequest mLocationRequest;
    private AtomicReference<Location> mRequestLocation;

    public Location getLocation( GoogleApiClient googleApiClient) {
        try {
            AppInit.configureLocationApi( googleApiClient );
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation( googleApiClient );
            log.debug( "getLocation: lastLocation=" + lastLocation );

            if ( isLastLocationValid( lastLocation ) ) {
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
                try {
                    int i = 45; // Android SyncManager terminate thread after 60 seconds with no internet usage
                    while ( i-- > 0 && mRequestLocation.get() == null ) {
                        log.debug( "getLocation: sleeping " + i );
                        Thread.sleep( 1000 );
                    }
                } catch (InterruptedException e) {
                    log.warn( "getLocation: ", e );
                    Thread.currentThread().interrupt();
                }
                handlerThread.quitSafely();
                LocationServices.FusedLocationApi.removeLocationUpdates( googleApiClient, this );
                log.debug( "getLocation: mRequestLocation=" + mRequestLocation.get() );
                Location requestLocation = mRequestLocation.get();
                Location resLocation = requestLocation != null ? requestLocation : lastLocation;
                log.debug( "getLocation: returning=" + resLocation );
                return resLocation;
            }
        } catch (SecurityException e) {
            log.warn( "getLocation: ", e );
        }
        return null;
    }

    @Override
    public void onLocationChanged( Location location ) {
        log.debug( "onLocationChanged: " + location );
        mRequestLocation.set(location);
    }

    private boolean isLastLocationValid( Location lastLocation ) {
        if (lastLocation == null) {
            log.debug( "isLastLocationValid: lastLocation=null" );
            return false;
        }

        long lastLocationAgeMillis = System.currentTimeMillis() - lastLocation.getTime();
        boolean validLastLocation = lastLocationAgeMillis < MAX_AGE_LAST_LOCATION_MILLIS;
        log.debug( "getLocation: lastLocationAge=" + (lastLocationAgeMillis/(1000*60)) + "min validLastLocation=" + validLastLocation );
        return validLastLocation;
    }
}
