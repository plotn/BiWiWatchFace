package sca.biwiwatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.WindowInsets;

import java.util.Locale;

public class SecondFaceElement extends AbstractFaceElement {

    private static final String MEASURE_STRING = "00";

    private Paint mSecondPaint;

    private float mSecondHalfHeight;

    public SecondFaceElement( Context context) {
        super(context);
        int textColor = getColor(R.color.second );
        Typeface typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        mSecondPaint = new Paint();
        mSecondPaint.setColor(textColor);
        mSecondPaint.setTypeface(typefaceMedium);
        mSecondPaint.setTextAlign(Paint.Align.RIGHT);

        setAntiAlias(true);
    }

    public void setAntiAlias(boolean enabled) {
        mSecondPaint.setAntiAlias(enabled);
    }

    public void onApplyWindowInsets(WindowInsets insets) {
        boolean isRound = insets.isRound();
        float textSize = getDimension(isRound ? R.dimen.digital_second_size_round : R.dimen.digital_second_size);
        mSecondPaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mSecondPaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcBounds );
        mSecondHalfHeight = rcBounds.height() / 2.0f;
    }

    public void drawTime(Canvas canvas, Time time, int x, int y) {
        if ( isInInteractiveMode() ) {
            String szSecond = String.format( Locale.US, "%02d", time.second );
            canvas.drawText( szSecond, x, y + mSecondHalfHeight, mSecondPaint );
        }
    }
}
