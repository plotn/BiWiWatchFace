package sca.biwiwatchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.WindowInsets;

public class DatePaint {

    private static final String MEASURE_STRING = "Lj";

    private Context mContext;

    private Paint mDatePaint;

    private float mDateHalfHeight;

    public DatePaint(Context context) {
        mContext = context;
        Resources resources = mContext.getResources();
        Resources.Theme theme = mContext.getTheme();
        int textColor = resources.getColor(R.color.date, theme );
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
        Resources resources = mContext.getResources();
        boolean isRound = insets.isRound();
        float textSize = resources.getDimension(isRound ? R.dimen.digital_date_size_round : R.dimen.digital_date_size);
        mDatePaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mDatePaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcBounds );
        mDateHalfHeight = rcBounds.height() / 2.0f;
    }

    public void drawTime(Canvas canvas, Time time, int x, int y) {
        String szDate = time.format("%a %d %b");
        canvas.drawText( szDate, x, y + mDateHalfHeight, mDatePaint );
    }

}
