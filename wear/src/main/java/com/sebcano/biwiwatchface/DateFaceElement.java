package com.sebcano.biwiwatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.WindowInsets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateFaceElement extends AbstractFaceElement {

    private static final String MEASURE_STRING = "Lj";

    private Paint mDatePaint;
    private DateFormat mDateFormat = new SimpleDateFormat("EEE d MMM", Locale.getDefault());

    private float mDateHalfHeight;

    public DateFaceElement( Context context) {
        super( context );
        int textColor = getColor(R.color.date);
        Typeface typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        mDatePaint = new Paint();
        mDatePaint.setColor(textColor);
        mDatePaint.setTypeface(typefaceMedium);
        mDatePaint.setTextAlign(Paint.Align.CENTER);

        setAntiAlias(true);
    }

    public void setAntiAlias(boolean enabled) {
        mDatePaint.setAntiAlias(enabled);
    }

    public void onApplyWindowInsets(WindowInsets insets) {
        float textSize = getDimension( R.dimen.digital_date_size );
        mDatePaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mDatePaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcBounds );
        mDateHalfHeight = rcBounds.height() / 2.0f;
    }

    private String mCachedDateString;
    private int mLastDayOfYear;

    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {
        if (isInInteractiveMode()) {
            int dayOfYear = calendar.get( Calendar.DAY_OF_YEAR );
            if (mCachedDateString == null || mLastDayOfYear != dayOfYear) {
                mCachedDateString = mDateFormat.format(calendar.getTime());
                mLastDayOfYear = dayOfYear;
            }
            canvas.drawText( mCachedDateString, x, y + mDateHalfHeight, mDatePaint );
        }
    }

}
