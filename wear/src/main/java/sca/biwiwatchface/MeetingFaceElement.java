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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.WindowInsets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MeetingFaceElement extends AbstractFaceElement {

    private static final String DATE_MEASURE_STRING = "Lj";

    final TextPaint mTitlePaint;
    final TextPaint mDatePaint;
    ArrayList<Meeting> mLastValue = null;
    ArrayList<PositionedMeeting> mlstPositionedMeetings = new ArrayList<>();
    MeetingInfo mMeetingInfo;
    private float mLineHeigth;
    FaceBoundComputer mFaceLayout = new FaceBoundComputer();

    public MeetingFaceElement( Context context ) {
        super( context );

        int textColor = getColor( R.color.calendar );
        Typeface typefaceNormal = Typeface.create("sans-serif", Typeface.NORMAL);
        mTitlePaint = new TextPaint();
        mTitlePaint.setColor(textColor);
        mTitlePaint.setTypeface(typefaceNormal);

        Typeface typefaceBold = Typeface.create("sans-serif", Typeface.BOLD);
        mDatePaint = new TextPaint();
        mDatePaint.setColor(textColor);
        mDatePaint.setTypeface(typefaceBold);

        setAntiAlias( true );

        mMeetingInfo = new MeetingInfo( context );
    }

    @Override
    public void setAntiAlias( boolean enabled ) {
        mTitlePaint.setAntiAlias( enabled );
        mDatePaint.setAntiAlias( enabled );
    }

    @Override
    public void onApplyWindowInsets( WindowInsets insets ) {
        boolean isRound = insets.isRound();

        float textSize = getDimension(isRound ? R.dimen.digital_calendar_size_round : R.dimen.digital_calendar_size);
        mTitlePaint.setTextSize(textSize);
        mDatePaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mDatePaint.getTextBounds(DATE_MEASURE_STRING, 0, DATE_MEASURE_STRING.length(), rcBounds );
        mLineHeigth = rcBounds.height();
    }

    private static int getTextWidth( TextPaint paint, String text ) {
        Rect rcBounds = new Rect();
        paint.getTextBounds( text, 0, text.length(), rcBounds );
        return rcBounds.width();
    }

    @Override
    public void drawTime( Canvas canvas, Calendar calendar, FaceBoundComputer boundComputer, int x, int y ) {
        ArrayList<Meeting> currentValue = mMeetingInfo.getLstMeetings();

        if ( currentValue != mLastValue ) {
            mLastValue = currentValue;
            mlstPositionedMeetings.clear();
            int currentY = y;
            for (Meeting meeting : currentValue) {
                String begin = meeting.mBeginDate;
                int dateWidth = getTextWidth( mDatePaint, begin+"x" );
                int xBoundLeft = boundComputer.getLeftSide( currentY );
                mlstPositionedMeetings.add( new PositionedMeeting( meeting, xBoundLeft, xBoundLeft+dateWidth, currentY ) );
                currentY += mLineHeigth;
            }
        }

        if ( mlstPositionedMeetings != null ) {
            for ( PositionedMeeting positionedMeeting : mlstPositionedMeetings ) {
                canvas.drawText( positionedMeeting.mMeting.mBeginDate, positionedMeeting.mXDate, positionedMeeting.mY, mDatePaint );
                canvas.drawText( positionedMeeting.mMeting.mTitle, positionedMeeting.mXTitle, positionedMeeting.mY, mTitlePaint );
            }
        }
    }

    @Override
    public void startSync( ScheduledExecutorService executorService ) {
        if (isInInteractiveMode()) {
            mMeetingInfo.startSync( executorService );
        }
    }

    @Override
    public void stopSync() {
        mMeetingInfo.stopSync();
    }

    private static class PositionedMeeting {
        private Meeting mMeting;
        private int mXDate;
        private int mXTitle;
        private int mY;

        PositionedMeeting( Meeting meeting, int xDate, int xTitle, int y) {
            mMeting = meeting;
            mXDate = xDate;
            mXTitle = xTitle;
            mY = y;
        }
    }


    private static class Meeting {
        private String mBeginDate;
        private String mTitle;

        public Meeting( String beginDate, String title ) {
            mBeginDate = beginDate;
            mTitle = title;
        }

        @Override
        public boolean equals( Object o ) {
            if ( ! (o instanceof Meeting) ) return false;
            Meeting otherMeeting = (Meeting) o;
            return mBeginDate.equals( otherMeeting.mBeginDate )
                    && mTitle.equals( otherMeeting.mTitle );
        }
    }


    private static class MeetingInfo {
        private static final String LOG_TAG = MeetingInfo.class.getSimpleName();

        private AtomicReference<ArrayList<Meeting>> mLstMeetings = new AtomicReference<>();

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
            mLoaderRunnable = new MeetingLoader( mContext, mLstMeetings );
        }

        public ArrayList<Meeting> getLstMeetings() {
            return mLstMeetings.get();
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
            private AtomicReference<ArrayList<Meeting>> mLstMeetings;

            private DateFormat mTimeFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
            private PowerManager.WakeLock mWakeLock;

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

            @Override
            public void run() {
                Log.d( LOG_TAG, "run" );
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
                }

                wakeLock.release();
                Log.d( LOG_TAG, "run complete" );
            }
        }

    }

}
