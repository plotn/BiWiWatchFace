package sca.biwiwatchface;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.WindowInsets;

import java.util.Locale;

public class BatteryPaint {
    private static final String MEASURE_STRING = "00%";

    private Context mContext;

    private Paint mBatteryPaint;
    private Bitmap mScaledBitmap;
    private Paint mBitmapPaint;

    private float mBatteryHalfHeight;
    private int mScaledBmpWidth;
    private int mScaledBmpHeight;

    private IntentFilter mBatteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    public BatteryPaint(Context context) {
        mContext = context;
        Resources resources = mContext.getResources();
        Resources.Theme theme = mContext.getTheme();
        int textColor = resources.getColor(R.color.battery, theme );
        Typeface typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        mBatteryPaint = new Paint();
        mBatteryPaint.setColor(textColor);
        mBatteryPaint.setTypeface(typefaceMedium);
        mBatteryPaint.setTextAlign(Paint.Align.LEFT);

        float[] colorTransform = {
                0, Color.red(textColor)/255f, 0, 0, 0,
                0, 0, Color.green(textColor)/255f, 0, 0,
                0, 0, 0, Color.blue(textColor)/255f, 0,
                0, 0, 0, 1f, 0};
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(colorTransform);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        mBitmapPaint = new Paint();
        mBitmapPaint.setColorFilter(colorFilter);

        setAntiAlias(true);
    }

    public void setAntiAlias(boolean enabled) {
        mBatteryPaint.setAntiAlias(enabled);
        mBitmapPaint.setAntiAlias(enabled);
    }

    public void onApplyWindowInsets(WindowInsets insets) {
        Resources resources = mContext.getResources();
        boolean isRound = insets.isRound();
        float textSize = resources.getDimension(isRound ? R.dimen.digital_battery_size_round : R.dimen.digital_battery_size);
        mBatteryPaint.setTextSize(textSize);

        Rect rcTextBounds = new Rect();
        mBatteryPaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcTextBounds );
        mBatteryHalfHeight = rcTextBounds.height() / 2.0f;

        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_battery_std_white_18dp);
        int srcBmpWidth = bitmap.getWidth();
        int srcBmpHeight = bitmap.getHeight();
        float ratio = 1;//(float)rcTextBounds.height() / srcBmpHeight;
        mScaledBmpWidth = Math.round(srcBmpWidth*ratio);
        mScaledBmpHeight = Math.round(srcBmpHeight*ratio);
        mScaledBitmap = Bitmap.createScaledBitmap( bitmap, mScaledBmpWidth, mScaledBmpHeight, false);
    }

    public void draw(Canvas canvas, int x, int y) {
        canvas.drawBitmap( mScaledBitmap, x, y-mScaledBmpHeight/2, mBitmapPaint );

        Intent batteryStatus = mContext.registerReceiver(null, mBatteryIntentFilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = Math.round( 100*level / (float)scale );
        String szBattery = String.format(Locale.US, "%02d%%", batteryPct );
        canvas.drawText( szBattery, x + mScaledBmpWidth, y + mBatteryHalfHeight, mBatteryPaint);

    }

}
