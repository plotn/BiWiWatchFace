package sca.biwiwatchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.WindowInsets;

public class TimePaint {
    private static final String COLON_STRING = ":";

    private Context mContext;

    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mColonPaint;

    private float mColonHalfWidth;

    public TimePaint(Context context) {
        mContext = context;
        Resources resources = mContext.getResources();
        Resources.Theme theme = mContext.getTheme();
        int textColor = resources.getColor(R.color.digital_text, theme );
        Typeface typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

        mHourPaint = new Paint();
        mHourPaint.setColor(textColor);
        mHourPaint.setTypeface(typeface);
        mHourPaint.setTextAlign(Paint.Align.RIGHT);

        mMinutePaint = new Paint();
        mMinutePaint.setColor(textColor);
        mMinutePaint.setTypeface(typeface);
        mMinutePaint.setTextAlign(Paint.Align.LEFT);

        mColonPaint = new Paint();
        mColonPaint.setColor(textColor);
        mColonPaint.setTypeface(typeface);
        mColonPaint.setTextAlign(Paint.Align.CENTER);

        setAntiAlias(true);
    }

    public void onApplyWindowInsets(WindowInsets insets) {
        Resources resources = mContext.getResources();
        boolean isRound = insets.isRound();
        float textSize = resources.getDimension(isRound
                ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
        mHourPaint.setTextSize(textSize);
        mMinutePaint.setTextSize(textSize);
        mColonPaint.setTextSize(textSize);

        mColonHalfWidth = mColonPaint.measureText(COLON_STRING)/2;
    }

    public void setAntiAlias(boolean enabled) {
        mHourPaint.setAntiAlias(enabled);
        mMinutePaint.setAntiAlias(enabled);
        mColonPaint.setAntiAlias(enabled);
    }

    public void drawTime(Canvas canvas, Time time, int x, int y) {
        String szHour = Integer.toString(time.hour);
        String szMinute = String.format("%02d", time.minute);
        float hourWidth = mHourPaint.measureText(szHour);
        float minuteWidth = mMinutePaint.measureText(szMinute);
        float hmDeltaWidth = hourWidth - minuteWidth;
        float xColonPos = x + hmDeltaWidth/2;

        canvas.drawText( szHour, xColonPos - mColonHalfWidth, y, mHourPaint );
        if ( time.second % 2 == 0 ) {
            canvas.drawText(COLON_STRING, xColonPos, y, mColonPaint);
        }

        canvas.drawText( szMinute, xColonPos + mColonHalfWidth, y, mMinutePaint );
    }
}
