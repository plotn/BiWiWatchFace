package com.sebcano.biwiwatchface.sync;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();
    
    // Storage for an instance of the sync adapter
    private static SyncAdapter sSyncAdapter = null;

    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    /*
     * Instantiate the sync adapter object.
     */
    @Override
    public void onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if ( sSyncAdapter == null ) {
                sSyncAdapter = new SyncAdapter( getApplicationContext(), true );
            }
        }
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     */
    @Override
    public IBinder onBind( Intent intent ) {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */
        return sSyncAdapter.getSyncAdapterBinder();
    }


    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "com.sebcano.biwiwatchface.provider";


    public static final long SYNC_INTERVAL_IN_SECONDS = 2*60*60; // Minimun seems to be 60 seconds

    public static void startSyncing(Context context) {
        Log.d( TAG, "startSyncing: " );
        Account account = Authenticator.getSyncAccount( context );
        startSyncing( account );
    }

    public static void startSyncing(Account account) {
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync( account, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL_IN_SECONDS );
    }

    public static void syncNow( Context context ) {
        Log.d( TAG, "syncNow: " );
        Account account = Authenticator.getSyncAccount( context );

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
    }
}
