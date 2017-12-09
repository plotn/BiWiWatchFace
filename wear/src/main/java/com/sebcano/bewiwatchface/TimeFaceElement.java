package com.sebcano.bewiwatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

public class TimeFaceElement extends AbstractFaceElement {
    private static final String COLON_STRING = ":";
    private static final String DIGIT_STRING = "00";

    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mColonPaint;

    private float mColonHalfWidth;
    private float mColonHalfHeight;
    private float mDigitHalfHeight;

    private boolean mIs24HourFormat;

    public TimeFaceElement( Context context) {
        super (context);
        int textColor = getColor(R.color.digital_time );
        Typeface typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        mHourPaint = new Paint();
        mHourPaint.setColor(textColor);
        mHourPaint.setTypeface(typefaceMedium);
        mHourPaint.setTextAlign(Paint.Align.RIGHT);

        mMinutePaint = new Paint();
        mMinutePaint.setColor(textColor);
        mMinutePaint.setTypeface(typefaceMedium);
        mMinutePaint.setTextAlign(Paint.Align.LEFT);

        Typeface typefaceNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        mColonPaint = new Paint();
        mColonPaint.setColor(textColor);
        mColonPaint.setTypeface(typefaceNormal);
        mColonPaint.setTextAlign(Paint.Align.CENTER);

        setAntiAlias(true);
    }

    @Override
    public void setTextSize(float textSize) {
        mHourPaint.setTextSize(textSize);
        mMinutePaint.setTextSize(textSize);
        mColonPaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mColonPaint.getTextBounds(COLON_STRING, 0, COLON_STRING.length(), rcBounds );
        mColonHalfWidth = rcBounds.width();
        mColonHalfHeight = rcBounds.height() / 2.0f;

        mMinutePaint.getTextBounds(DIGIT_STRING, 0, DIGIT_STRING.length(), rcBounds );
        mDigitHalfHeight = rcBounds.height() / 2.0f;
    }

    @Override
    public void setAntiAlias(boolean enabled) {
        mHourPaint.setAntiAlias(enabled);
        mMinutePaint.setAntiAlias(enabled);
        mColonPaint.setAntiAlias(enabled);
    }

    private CachedStringFromLong mCachedHourString = new CachedStringFromLong( new CachedStringFromLong.StringFormatter() {
        @Override
        public String computeString( long value ) {
            return Long.toString( value );
        }
    } );

    private CachedStringFromLong mCachedMinuteString = new CachedStringFromLong( new CachedStringFromLong.StringFormatter() {
        @Override
        public String computeString( long value ) {
            return String.format(Locale.US, "%02d", value );
        }
    } );

    @Override
    public void startSync( ScheduledExecutorService executor ) {
        mIs24HourFormat = DateFormat.is24HourFormat( getContext() );
    }

    @Override
    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {
        int hour;
        if (mIs24HourFormat) {
            hour = calendar.get(Calendar.HOUR_OF_DAY);
        } else {
            hour = calendar.get(Calendar.HOUR);
            if (hour ==0) hour = 12;
        }
        String szHour = mCachedHourString.getString( hour );
        String szMinute = mCachedMinuteString.getString( calendar.get(Calendar.MINUTE) );

        float hourWidth = mHourPaint.measureText(szHour);
        float minuteWidth = mMinutePaint.measureText(szMinute);
        float hmDeltaWidth = hourWidth - minuteWidth;
        float xColonPos = x + hmDeltaWidth/2;
        float yDigit = y + mDigitHalfHeight;

        canvas.drawText( szHour, xColonPos - mColonHalfWidth, yDigit, mHourPaint );
        if ( isInAmbientMode() || calendar.get(Calendar.SECOND) % 2 == 0 ) {
            canvas.drawText(COLON_STRING, xColonPos, y + mColonHalfHeight, mColonPaint);
        }

        canvas.drawText( szMinute, xColonPos + mColonHalfWidth, yDigit, mMinutePaint );
    }
}
