package sca.biwiwatchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.WindowInsets;

import java.util.Locale;

public class SecondPaint {

    private static final String MEASURE_STRING = "00";

    private Context mContext;

    private Paint mSecondPaint;

    private float mSecondHalfHeight;

    public SecondPaint(Context context) {
        mContext = context;
        Resources resources = mContext.getResources();
        Resources.Theme theme = mContext.getTheme();
        int textColor = resources.getColor(R.color.second, theme );
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
        Resources resources = mContext.getResources();
        boolean isRound = insets.isRound();
        float textSize = resources.getDimension(isRound ? R.dimen.digital_second_size_round : R.dimen.digital_second_size);
        mSecondPaint.setTextSize(textSize);

        Rect rcBounds = new Rect();
        mSecondPaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcBounds );
        mSecondHalfHeight = rcBounds.height() / 2.0f;
    }

    public void drawTime(Canvas canvas, Time time, int x, int y) {
        String szSecond = String.format(Locale.US, "%02d", time.second);
        canvas.drawText( szSecond, x, y+mSecondHalfHeight, mSecondPaint);
    }
}
