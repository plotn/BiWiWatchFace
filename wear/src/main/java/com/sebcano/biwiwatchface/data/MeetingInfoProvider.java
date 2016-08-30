package com.sebcano.biwiwatchface.data;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sebcano.biwiwatchface.R;

public class MeetingInfoProvider {
    private static final String LOG_TAG = MeetingInfoProvider.class.getSimpleName();

    public enum CalendarPermission {
        UNKNOWN, GRANTED, DENIED
    }

    public interface ChangeListener {
        void onChange();
    }

    private CalendarPermission mCalendarPermission = CalendarPermission.UNKNOWN;

    private AtomicReference<ArrayList<Meeting>> mLstMeetings = new AtomicReference<>();
    private ChangeListener mChangeListener;

    private Context mContext;
    ScheduledExecutorService mExecutor;
    private boolean syncing;

    private MeetingLoader mLoader;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())
                    && WearableCalendarContract.CONTENT_URI.equals(intent.getData())) {
                Log.d( LOG_TAG, "onReceive" );
                mExecutor.execute( mLoader );
            }
        }
    };

    public MeetingInfoProvider( Context context ) {
        mContext = context;
        mLoader = new MeetingLoader( mContext, mLstMeetings );
    }

    public CalendarPermission getCalendarPermission() {
        return mCalendarPermission;
    }

    public ArrayList<Meeting> getLstMeetings() {
        return mLstMeetings.get();
    }

    public void setChangeListener( ChangeListener changeListener ) {
        mChangeListener = changeListener;
        mLoader.setChangeListener( changeListener );
    }

    public void startSync( ScheduledExecutorService executor ) {
        Log.d( LOG_TAG, "startSync" );
        mExecutor = executor;
        boolean permissionGranted = ActivityCompat.checkSelfPermission( mContext, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted && ! syncing) {
            mCalendarPermission = CalendarPermission.GRANTED;
            IntentFilter filter = new IntentFilter( Intent.ACTION_PROVIDER_CHANGED );
            filter.addDataScheme( "content" );
            filter.addDataAuthority( WearableCalendarContract.AUTHORITY, null );
            mContext.registerReceiver( mBroadcastReceiver, filter );
            mExecutor.scheduleWithFixedDelay( mLoader, 0, 60, TimeUnit.SECONDS );
            syncing = true;
        } else if (! permissionGranted) {
            mCalendarPermission = CalendarPermission.DENIED;
        }
    }

    public void stopSync( ) {
        Log.d( LOG_TAG, "stopSync" );
        if (syncing) {
            mExecutor = null;
            mContext.unregisterReceiver( mBroadcastReceiver );
            syncing = false;
        }
    }

    private static class MeetingLoader implements Runnable {
        private Context mContext;
        private AtomicReference<ArrayList<Meeting>> mLstMeetings;
        private ChangeListener mChangeListener;

        private DateFormat mTimeFormat = new SimpleDateFormat("H:mm", Locale.getDefault());

        private static final String[] PROJECTION = {
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.ALL_DAY
        };
        private static final int COL_INSTANCE_BEGIN = 0;
        private static final int COL_INSTANCE_TITLE = 1;
        private static final int COL_INSTANCE_ALL_DAY = 2;

        MeetingLoader(Context context, AtomicReference<ArrayList<Meeting>> lstMeetings) {
            mContext = context;
            mLstMeetings = lstMeetings;
        }

        public void setChangeListener( ChangeListener changeListener ) {
            mChangeListener = changeListener;
        }

        @Override
        public void run() {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "CalendarWatchFaceWakeLock");
            wakeLock.acquire();

            long now = System.currentTimeMillis();
            Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId( builder, now );
            ContentUris.appendId( builder, now + DateUtils.DAY_IN_MILLIS);
            Uri calendarUri = builder.build();

            final Cursor cursor = mContext.getContentResolver().query( calendarUri, PROJECTION, null, null, null );

            if (cursor != null) {
                ArrayList<Meeting> lstAllDayMeetings = new ArrayList<>();
                ArrayList<Meeting> lstInDayMeetings = new ArrayList<>();
                while ( cursor.moveToNext() ) {
                    String instanceTitle = cursor.getString( COL_INSTANCE_TITLE );
                    boolean isAllDay = cursor.getInt( COL_INSTANCE_ALL_DAY ) == 1;
                    long instanceBegin = cursor.getLong( COL_INSTANCE_BEGIN );
                    if ( isAllDay ) {
                        int idPrefixString = instanceBegin > now ? R.string.tomorrow : R.string.today;
                        lstAllDayMeetings.add( new Meeting( mContext.getString( idPrefixString ), instanceTitle ) );
                    } else {
                        Date startDate = new Date( instanceBegin );
                        lstInDayMeetings.add( new Meeting( mTimeFormat.format( startDate ), instanceTitle ));
                    }
                }
                cursor.close();
                lstInDayMeetings.addAll( lstAllDayMeetings );
                mLstMeetings.set( lstInDayMeetings );

                if (mChangeListener != null) mChangeListener.onChange();
            }

            wakeLock.release();
        }
    }

}