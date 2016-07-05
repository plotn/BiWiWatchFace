package sca.biwiwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* Emulator launch:
telnet localhost 5554
auth /MlEbZ6a8HZ5jilz
redir add tcp:5601:5601

 */

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MainWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(MainWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private final String LOG_TAG = Engine.class.getSimpleName();

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        List<AbstractFaceElement> mLstFaceElement;
        TimeFaceElement mTimePaint;
        DateFaceElement mDatePaint;
        SecondFaceElement mSecondPaint;
        BatteryFaceElement mBatteryPaint;
        CalendarFaceElement mCalendarPaint;

        private ScheduledExecutorService mExecutorService;

        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d( LOG_TAG, "onCreate" );
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MainWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MainWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background, getTheme()));

            Context context = MainWatchFace.this;
            mLstFaceElement = Arrays.asList(
                    mTimePaint = new TimeFaceElement( context ),
                    mSecondPaint = new SecondFaceElement( context ),
                    mDatePaint = new DateFaceElement( context ),
                    mBatteryPaint = new BatteryFaceElement( context ),
                    mCalendarPaint = new CalendarFaceElement( context )
            );

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            Log.d( LOG_TAG, "onDestroy" );
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            stopExecutorService();
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d( LOG_TAG, "onVisibilityChanged " + visible );
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                startExecutorService();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                stopExecutorService();
                unregisterReceiver();
            }

            for (AbstractFaceElement element : mLstFaceElement) {
                element.onVisibilityChanged( visible );
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MainWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MainWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d( LOG_TAG, "onApplyWindowInsets");
            super.onApplyWindowInsets(insets);

            for (AbstractFaceElement element : mLstFaceElement) {
                element.onApplyWindowInsets( insets );
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            Log.d( LOG_TAG, "onPropertiesChanged");
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.d( LOG_TAG, "onAmbientModeChanged " + inAmbientMode);
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.onAmbientModeChanged( mAmbient, mLowBitAmbient );
                }
                stopExecutorService();
                startExecutorService();
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MainWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    //mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                    //        R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d( LOG_TAG, "onDraw");
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            mTime.setToNow();
            mTimePaint.drawTime( canvas, mTime, bounds.centerX(), bounds.centerY() );
            if (!mAmbient) {
                mDatePaint.drawTime(canvas, mTime, bounds.centerX(), bounds.centerY() - 80);
                mSecondPaint.drawTime( canvas, mTime, bounds.right - 30, bounds.centerY() + 60 );
                mBatteryPaint.drawTime( canvas, mTime, bounds.left + 30, bounds.centerY() + 60 );
                mCalendarPaint.drawTime( canvas, mTime, bounds.centerX(), bounds.centerY() + 80 );
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void startExecutorService() {
            if (null == mExecutorService) {
                mExecutorService = Executors.newSingleThreadScheduledExecutor();
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.startExecutorService( mExecutorService );
                }
            }
        }

        private void stopExecutorService() {
            if (null != mExecutorService) {
                mExecutorService.shutdownNow();
                mExecutorService = null;
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.stopExecutorService();
                }
            }
        }

    }
}
