package sca.biwiwatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.WindowInsets;

public class DateFaceElement extends AbstractFaceElement {

    private static final String MEASURE_STRING = "Lj";

    private Paint mDatePaint;

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
        boolean isRound = insets.isRound();
        float textSize = getDimension(isRound ? R.dimen.digital_date_size_round : R.dimen.digital_date_size);
        mDatePaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mDatePaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcBounds );
        mDateHalfHeight = rcBounds.height() / 2.0f;
    }

    public void drawTime(Canvas canvas, Time time, int x, int y) {
        if (isInInteractiveMode()) {
            String szDate = time.format("%a %d %b");
            canvas.drawText( szDate, x, y + mDateHalfHeight, mDatePaint );
        }
    }

}
