package sca.biwiwatchface;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.WindowInsets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CalendarFaceElement extends AbstractFaceElement {

    final TextPaint mCalendarPaint;
    String mLastValue = "";
    final Editable mEditable = new SpannableStringBuilder();
    DynamicLayout mLayout;
    MeetingInfo mMeetingInfo;

    public CalendarFaceElement( Context context ) {
        super( context );

        int textColor = getColor( R.color.calendar );
        Typeface typefaceMedium = Typeface.create("sans-serif", Typeface.NORMAL);

        mCalendarPaint = new TextPaint();
        mCalendarPaint.setColor(textColor);
        mCalendarPaint.setTypeface(typefaceMedium);
        mLayout = new DynamicLayout( mEditable, mCalendarPaint, 190,  Layout.Alignment.ALIGN_NORMAL, 1, 0, false );

        setAntiAlias( true );

        mMeetingInfo = new MeetingInfo( context );
    }

    @Override
    public void setAntiAlias( boolean enabled ) {
        mCalendarPaint.setAntiAlias( enabled );
    }

    @Override
    public void onApplyWindowInsets( WindowInsets insets ) {
        boolean isRound = insets.isRound();
        float textSize = getDimension(isRound ? R.dimen.digital_calendar_size_round : R.dimen.digital_calendar_size);
        mCalendarPaint.setTextSize(textSize);
    }

    @Override
    public void drawTime( Canvas canvas, Time time, int x, int y ) {
        String currentValue = mMeetingInfo.getMeetingHtml();

        if (!currentValue.equals( mLastValue )) {
            mEditable.clear();
            mEditable.append( Html.fromHtml(currentValue) );
            mLastValue = currentValue;
        }
        canvas.save();
        canvas.translate( x-mLayout.getWidth()/2, y );
        mLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    public void startExecutorService( ScheduledExecutorService executorService ) {
        if (isInInteractiveMode()) {
            mMeetingInfo.startSync( executorService );
        }
    }

    @Override
    public void stopExecutorService() {
        mMeetingInfo.stopSync();
    }


    private static class MeetingInfo {
        private static final String LOG_TAG = MeetingInfo.class.getSimpleName();

        private AtomicReference<String> mMeetingHtml = new AtomicReference<>("");

        private Context mContext;
        ScheduledExecutorService mExecutor;
        private boolean syncing;

        private Runnable mLoaderRunnable;
        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())
                        && WearableCalendarContract.CONTENT_URI.equals(intent.getData())) {
                    Log.d( LOG_TAG, "onReceive" );
                    mExecutor.execute( mLoaderRunnable );
                }
            }
        };

        public MeetingInfo( Context context ) {
            mContext = context;
            mLoaderRunnable = new MeetingLoader( mContext, mMeetingHtml );
        }

        public String getMeetingHtml() {
            return mMeetingHtml.get();
        }

        public void startSync( ScheduledExecutorService executor ) {
            Log.d( LOG_TAG, "startSync" );
            mExecutor = executor;
            boolean permissionGranted = ActivityCompat.checkSelfPermission( mContext, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;

            if (permissionGranted && ! syncing) {
                IntentFilter filter = new IntentFilter( Intent.ACTION_PROVIDER_CHANGED );
                filter.addDataScheme( "content" );
                filter.addDataAuthority( WearableCalendarContract.AUTHORITY, null );
                mContext.registerReceiver( mBroadcastReceiver, filter );
                mExecutor.scheduleWithFixedDelay( mLoaderRunnable, 0, 60, TimeUnit.SECONDS );
                syncing = true;
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
            private AtomicReference<String> mMeetingHtml;

            private PowerManager.WakeLock mWakeLock;

            private static final String[] PROJECTION = {
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.ALL_DAY
            };
            private static final int COL_INSTANCE_BEGIN = 0;
            private static final int COL_INSTANCE_TITLE = 1;
            private static final int COL_INSTANCE_ALL_DAY = 2;

            MeetingLoader(Context context, AtomicReference<String> meetingHtml) {
                mContext = context;
                mMeetingHtml = meetingHtml;
            }

            @Override
            public void run() {
                Log.d( LOG_TAG, "run" );
                PowerManager powerManager = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "CalendarWatchFaceWakeLock");
                wakeLock.acquire();

                long now = System.currentTimeMillis();
                Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId( builder, now );
                ContentUris.appendId( builder, now + DateUtils.DAY_IN_MILLIS);
                Uri calendarUri = builder.build();

                final Cursor cursor = mContext.getContentResolver().query( calendarUri, PROJECTION, null, null, null );

                StringBuilder sb = new StringBuilder();
                while (cursor.moveToNext()) {
                    String instanceTitle = cursor.getString( COL_INSTANCE_TITLE );
                    boolean isAllDay = cursor.getInt( COL_INSTANCE_ALL_DAY ) == 1;
                    long instanceBegin = cursor.getLong( COL_INSTANCE_BEGIN );
                    if (isAllDay) {
                        int idPrefixString = instanceBegin > now ? R.string.tomorrow : R.string.today;
                        sb.append( String.format( "<b>%s</b> %s<br/>", mContext.getString( idPrefixString ), instanceTitle ) );
                    } else {
                        Date startDate = new Date(instanceBegin);
                        DateFormat timeFormat = new SimpleDateFormat("H:mm");
                        sb.append( String.format("<b>%s:</b> %s<br/>", timeFormat.format(startDate), instanceTitle ) );
                    }
                }
                cursor.close();

                mMeetingHtml.set( sb.toString() );

                wakeLock.release();
            }
        }

    }

}
