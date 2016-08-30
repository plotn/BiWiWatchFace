package com.sebcano.biwiwatchface.debug;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;

public class MockLocationProvider {
    private static final String TAG = MockLocationProvider.class.getSimpleName();
    
    String providerName;
    Context ctx;

    public MockLocationProvider(String name, Context ctx) {
        this.providerName = name;
        this.ctx = ctx;

        LocationManager lm = (LocationManager) ctx.getSystemService( Context.LOCATION_SERVICE);
        lm.addTestProvider(providerName, false, false, false, false, false, true, true, 0, 5);
        lm.setTestProviderEnabled(providerName, true);
    }

    public void pushLocation(double lat, double lon) {
        LocationManager lm = (LocationManager) ctx.getSystemService( Context.LOCATION_SERVICE);

        Location mockLocation = getLocation( providerName, lat, lon );
        lm.setTestProviderLocation(providerName, mockLocation);
    }

    @NonNull
    public static Location getLocation(String providerName, double lat, double lon ) {
        Location mockLocation = new Location( providerName );
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lon);
        mockLocation.setAltitude(0);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setAccuracy( 100 );
        mockLocation.setElapsedRealtimeNanos( SystemClock.elapsedRealtimeNanos());
        return mockLocation;
    }


    public void shutdown() {
        LocationManager lm = (LocationManager) ctx.getSystemService( Context.LOCATION_SERVICE);
        lm.removeTestProvider(providerName);
    }
}
