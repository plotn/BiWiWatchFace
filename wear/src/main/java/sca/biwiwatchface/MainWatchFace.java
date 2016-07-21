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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sca.biwiwatchface.apiwrap.ProductionRectWrapper;

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
        private final String TAG = Engine.class.getSimpleName();

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        private ProductionRectWrapper mBoundRectWrapper = new ProductionRectWrapper();
        private FaceBoundComputer mBoundComputer = new FaceBoundComputer();

        private Paint mBackgroundPaint;
        private List<AbstractFaceElement> mLstFaceElement;
        private TimeFaceElement mTimePaint;
        private DateFaceElement mDatePaint;
        private SecondFaceElement mSecondPaint;
        private BatteryFaceElement mBatteryPaint;
        private MeetingFaceElement mCalendarPaint;
        private WeatherFaceElement mWeatherPaint;

        private ScheduledExecutorService mExecutorService;
        boolean mAmbient;

        private Calendar mCalendar;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
            }
        };
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        private MobileLink mMobileLink;

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d( TAG, "onCreate" );
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
                    mCalendarPaint = new MeetingFaceElement( context ),
                    mWeatherPaint = new WeatherFaceElement( context )
            );

            mCalendar = Calendar.getInstance();

            mMobileLink = new MobileLink();
        }

        @Override
        public void onDestroy() {
            Log.d( TAG, "onDestroy" );
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            stopSync();
            mMobileLink.onDestroy();
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d( TAG, "onVisibilityChanged " + visible );
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                startSync();
                mMobileLink.resume();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                mMobileLink.suspend();
                stopSync();
                unregisterReceiver();
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
            //Log.d( TAG, "onApplyWindowInsets");
            super.onApplyWindowInsets(insets);

            mBoundComputer.setDimensions( mBoundRectWrapper, insets.isRound(), insets.getSystemWindowInsetBottom() );

            for (AbstractFaceElement element : mLstFaceElement) {
                element.onApplyWindowInsets( insets );
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            //Log.d( TAG, "onPropertiesChanged");
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
            Log.d( TAG, "onAmbientModeChanged " + inAmbientMode);
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.onAmbientModeChanged( mAmbient, mLowBitAmbient );
                }
                if (mAmbient) mMobileLink.suspend();
                else mMobileLink.resume();
                stopSync();
                startSync();
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
            for (AbstractFaceElement element : mLstFaceElement) {
                element.onTapCommand( tapType, x, y, eventTime );
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            //Log.d( TAG, "onDraw");
            mBoundRectWrapper.setRect( bounds );

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mTimePaint.drawTime( canvas, mCalendar, bounds.centerX(), bounds.centerY() );
            if (!mAmbient) {
                mDatePaint.drawTime(canvas, mCalendar, bounds.centerX(), bounds.centerY() - 80);
                mSecondPaint.drawTime( canvas, mCalendar, bounds.right - 30, bounds.centerY() + 60 );
                mBatteryPaint.drawTime( canvas, mCalendar, bounds.left + 30, bounds.centerY() + 60 );
                mCalendarPaint.drawTime( canvas, mCalendar, mBoundComputer, bounds.centerX(), bounds.centerY() + 100 );
                mWeatherPaint.drawTime( canvas, mCalendar, bounds.centerX(), bounds.centerY() - 105 );
            }
//            if (mCalendar.get(Calendar.SECOND) % 10 == 0 ) {
//                mMobileLink.sendDataItemStart("/syncNow");
//            }
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

        private void startSync() {
            if (null == mExecutorService) {
                mExecutorService = Executors.newSingleThreadScheduledExecutor();
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.startSync( mExecutorService );
                }
            }
        }

        private void stopSync() {
            if (null != mExecutorService) {
                mExecutorService.shutdownNow();
                mExecutorService = null;
                for (AbstractFaceElement element : mLstFaceElement) {
                    element.stopSync();
                }
            }
        }


        private class MobileLink implements DataApi.DataListener,
                GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                ResultCallback<DataApi.DataItemResult> {

            private static final String TAG = "MobileLink";
            private GoogleApiClient mGoogleApiClient;


            public MobileLink() {
                mGoogleApiClient = new GoogleApiClient.Builder( MainWatchFace.this )
                        .addApi( Wearable.API)
                        .addConnectionCallbacks( this )
                        .addOnConnectionFailedListener( this )
                        .build();
            }

            public void suspend() {
                Log.d( TAG, "suspend: " );
                if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.disconnect();
                }
            }

            public void resume() {
                Log.d( TAG, "resume: " );
                if ( ! (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) ) {
                    mGoogleApiClient.connect();
                }
            }

            public void onDestroy() {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }

            @Override
            public void onConnected( @Nullable Bundle connectionHint ) {
                Log.d( TAG, "onConnected: " + connectionHint );
                // Now you can use the Data Layer API
                Wearable.DataApi.addListener(mGoogleApiClient, this);

                Uri test = Uri.parse( "wear://*/weather" );
                Wearable.DataApi.getDataItems( mGoogleApiClient, test )
                        .setResultCallback( new ResultCallback<DataItemBuffer>() {
                    @Override
                    public void onResult( @NonNull DataItemBuffer dataItems ) {
                        if (dataItems.getCount() == 1 ) {
                            DataItem cachedItem = dataItems.get(0);
                            Log.d( TAG, "onResult: cachedItem=" + cachedItem.getUri() );
                            mWeatherPaint.setDataItem( cachedItem );
                        } else {
                            sendDataItemStart("/faceStarted");
                            sendDataItemStart( "/syncNow" );
                        }
                        dataItems.release();
                    }
                } );
            }

            @Override
            public void onConnectionSuspended( int cause ) {
                Log.d( TAG, "onConnectionSuspended: " + cause);
            }

            @Override
            public void onConnectionFailed( @NonNull ConnectionResult result ) {
                Log.d( TAG, "onConnectionFailed: " + result);
            }

            @Override
            public void onDataChanged( DataEventBuffer dataEvents ) {
                for (DataEvent event : dataEvents) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        // DataItem changed
                        DataItem item = event.getDataItem();
                        if ( "/weather".equals( item.getUri().getPath() )) {
                            Log.d( TAG, "onDataChanged: path=" + item.getUri() );
                            mWeatherPaint.setDataItem( item );
                        }
                    }
                }
                dataEvents.release();
            }

            private void sendDataItemStart( String path ) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                request.setUrgent();

                Log.d( TAG, "Generating DataItem: " + request);
                Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                        .setResultCallback( this );
            }

            @Override
            public void onResult( @NonNull DataApi.DataItemResult dataItemResult ) {
                if (!dataItemResult.getStatus().isSuccess()) {
                    Log.e( TAG, "ERROR: failed to putDataItem, status code: "
                            + dataItemResult.getStatus().getStatusCode());
                }
            }
        }

    }


}
