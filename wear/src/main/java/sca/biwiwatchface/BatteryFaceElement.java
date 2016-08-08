package sca.biwiwatchface;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BatteryFaceElement extends AbstractFaceElement {
    private static final String MEASURE_STRING = "00%";

    private BatteryInfo mBatteryInfo;

    private Paint mBatteryPaint;
    private Bitmap mScaledBitmap;
    private Paint mBitmapPaint;

    private float mBatteryHalfHeight;
    private int mScaledBmpWidth;
    private int mScaledBmpHeight;

    private IntentFilter mBatteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    protected BatteryFaceElement( Context context ) {
        super(context);

        mBatteryInfo = new BatteryInfo(context);

        int textColor = getColor(R.color.battery );
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

    public void startSync( ScheduledExecutorService executorService ) {
        mBatteryInfo.startExecutorService( executorService );
    }

    public void setAntiAlias(boolean enabled) {
        mBatteryPaint.setAntiAlias(enabled);
        mBitmapPaint.setAntiAlias(enabled);
    }

    public void onApplyWindowInsets(WindowInsets insets) {
        boolean isRound = insets.isRound();
        float textSize = getDimension( R.dimen.digital_battery_size );
        mBatteryPaint.setTextSize(textSize);

        Rect rcTextBounds = new Rect();
        mBatteryPaint.getTextBounds(MEASURE_STRING, 0, MEASURE_STRING.length(), rcTextBounds );
        mBatteryHalfHeight = rcTextBounds.height() / 2.0f;

        Bitmap bitmap = getBitmap( R.drawable.ic_battery_std_white_18dp );
        int srcBmpWidth = bitmap.getWidth();
        int srcBmpHeight = bitmap.getHeight();
        float ratio = 1;//(float)rcTextBounds.height() / srcBmpHeight;
        mScaledBmpWidth = Math.round(srcBmpWidth*ratio);
        mScaledBmpHeight = Math.round(srcBmpHeight*ratio);
        mScaledBitmap = Bitmap.createScaledBitmap( bitmap, mScaledBmpWidth, mScaledBmpHeight, false);
    }

    private CachedStringFromLong mCachedBatteryString = new CachedStringFromLong( new CachedStringFromLong.StringFormatter() {
        @Override
        public String computeString( long value ) {
            return String.format( Locale.US, "%02d%%", value );
        }
    } );

    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {
        if (isInInteractiveMode()) {
            canvas.drawBitmap( mScaledBitmap, x, y - mScaledBmpHeight / 2, mBitmapPaint );

            int batteryPct = mBatteryInfo.getBatteryPct();
            String szBattery = mCachedBatteryString.getString( batteryPct );
            canvas.drawText( szBattery, x + mScaledBmpWidth, y + mBatteryHalfHeight, mBatteryPaint );
        }
    }

    private class BatteryInfo {
        private AtomicInteger mBatteryPct = new AtomicInteger();
        final private Context mContext;
        final private BatteryRunnable mBatteryRunnable = new BatteryRunnable();

        public BatteryInfo(Context context) {
            mContext = context;
        }

        public int getBatteryPct() {
            return mBatteryPct.get();
        }

        public void startExecutorService( ScheduledExecutorService executorService ) {
            if (isInInteractiveMode()) {
                executorService.scheduleWithFixedDelay( mBatteryRunnable, 0, 10, TimeUnit.SECONDS );
            }
        }

        private class BatteryRunnable implements Runnable {
            @Override
            public void run() {
                Intent batteryStatus = mContext.registerReceiver( null, mBatteryIntentFilter );
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra( BatteryManager.EXTRA_LEVEL, -1 );
                    int scale = batteryStatus.getIntExtra( BatteryManager.EXTRA_SCALE, -1 );
                    mBatteryPct.set( Math.round( 100 * level / (float) scale ) );
                }
            }
        }

    }

}
