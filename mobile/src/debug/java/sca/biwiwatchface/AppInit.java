package sca.biwiwatchface;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import sca.biwiwatchface.debug.MockLocationProvider;

public class AppInit {
    private static final String TAG = AppInit.class.getSimpleName();

    public static void onStartup(Context context) {
        Stetho.initializeWithDefaults(context);
    }

    public static void configureLocationApi( GoogleApiClient googleApiClient ) {
        try {
            LocationServices.FusedLocationApi.setMockMode( googleApiClient, true );
            Location mockLocation = MockLocationProvider.getLocation( LocationManager.NETWORK_PROVIDER, 45.193, 5.768 );
            LocationServices.FusedLocationApi.setMockLocation( googleApiClient, mockLocation  );
        } catch (SecurityException e) {
            Log.w( TAG, "onConnected: ", e );
        }
    }
}
