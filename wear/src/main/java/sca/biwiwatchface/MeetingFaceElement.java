package sca.biwiwatchface;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.wearable.watchface.WatchFaceService;
import android.text.TextPaint;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;

import sca.biwiwatchface.activities.CalendarPermissionActivity;
import sca.biwiwatchface.data.Meeting;
import sca.biwiwatchface.data.MeetingInfoProvider;
import sca.biwiwatchface.data.WideUnicode;

public class MeetingFaceElement extends AbstractFaceElement {

    private static final String DATE_MEASURE_STRING = "Lj";

    final TextPaint mTitlePaint;
    final TextPaint mDatePaint;
    ArrayList<Meeting> mLastValue = null;
    ArrayList<PositionedMeeting> mlstPositionedMeetings = new ArrayList<>();
    MeetingInfoProvider mMeetingInfoProvider;
    private float mLineHeight;
    private int mLastYDraw;

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

        mMeetingInfoProvider = new MeetingInfoProvider( context );
        mMeetingInfoProvider.setChangeListener( new MeetingInfoProvider.ChangeListener() {
            @Override
            public void onChange() {
                postInvalidate();
            }
        } );
    }

    @Override
    public void setAntiAlias( boolean enabled ) {
        mTitlePaint.setAntiAlias( enabled );
        mDatePaint.setAntiAlias( enabled );
    }

    @Override
    public void onApplyWindowInsets( WindowInsets insets ) {
        float textSize = getDimension( R.dimen.digital_calendar_size );
        mTitlePaint.setTextSize(textSize);
        mDatePaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mDatePaint.getTextBounds(DATE_MEASURE_STRING, 0, DATE_MEASURE_STRING.length(), rcBounds );
        mLineHeight = rcBounds.height();
    }

    private static int getTextWidth( TextPaint paint, String text ) {
        Rect rcBounds = new Rect();
        paint.getTextBounds( text, 0, text.length(), rcBounds );
        return rcBounds.width();
    }

    @Override
    public void drawTime( Canvas canvas, Calendar calendar, FaceBoundComputer boundComputer, int x, int y ) {
        switch (mMeetingInfoProvider.getCalendarPermission()) {
            case GRANTED:
                ArrayList<Meeting> currentValue = mMeetingInfoProvider.getLstMeetings();

                if ( currentValue != mLastValue ) {
                    mLastValue = currentValue;
                    mlstPositionedMeetings.clear();
                    int currentY = y;
                    for (Meeting meeting : currentValue) {
                        String begin = meeting.getBeginDate();
                        int dateWidth = getTextWidth( mDatePaint, begin+"x" );
                        int xBoundLeft = boundComputer.getLeftSide( currentY + (int)(mLineHeight /2) );
                        mlstPositionedMeetings.add( new PositionedMeeting( meeting, xBoundLeft, xBoundLeft+dateWidth, currentY ) );
                        currentY += mLineHeight;
                        if (! boundComputer.isYInScreen( currentY ) ) break;
                    }
                }

                if ( mlstPositionedMeetings != null ) {
                    for ( PositionedMeeting positionedMeeting : mlstPositionedMeetings ) {
                        canvas.drawText( positionedMeeting.mMeting.getBeginDate(), positionedMeeting.mXDate, positionedMeeting.mY, mDatePaint );
                        canvas.drawText( positionedMeeting.mMeting.getTitle(), positionedMeeting.mXTitle, positionedMeeting.mY, mTitlePaint );
                    }
                }

                break;

            case DENIED:
                int xBoundLeft = boundComputer.getLeftSide( y + (int)(mLineHeight /2) );
                canvas.drawText( WideUnicode.toString(0x1F4C5) + " " + WideUnicode.toString(0x1F512), xBoundLeft, y, mTitlePaint );
                break;
        }

        mLastYDraw = y;
    }

    public void onTapCommand(int tapType, int x, int y, long eventTime) {
        if ( tapType == WatchFaceService.TAP_TYPE_TAP && y > mLastYDraw-mLineHeight ) {
            switch (mMeetingInfoProvider.getCalendarPermission()) {
                case GRANTED:
                    Intent calendarIntent = new Intent();
                    calendarIntent.setAction(Intent.ACTION_MAIN);
                    calendarIntent.addCategory( Intent.CATEGORY_APP_CALENDAR );
                    startActivity(calendarIntent);
                    break;
                case DENIED:
                    Intent permissionIntent = new Intent( getContext(), CalendarPermissionActivity.class);
                    startActivity(permissionIntent);
                    break;
            }

        }
    }

    @Override
    public void startSync( ScheduledExecutorService executorService ) {
        if (isInInteractiveMode()) {
            mMeetingInfoProvider.startSync( executorService );
        }
    }

    @Override
    public void stopSync() {
        mMeetingInfoProvider.stopSync();
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



}
