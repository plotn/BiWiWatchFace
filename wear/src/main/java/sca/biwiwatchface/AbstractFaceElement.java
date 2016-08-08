package sca.biwiwatchface;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.wearable.watchface.WatchFaceService;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;

public abstract class AbstractFaceElement {
    private Context mContext;
    private Resources mResources;
    private boolean mInAmbientMode;
    private Rect mTapBounds;
    private InvalidateListener mInvalidateListener;

    public AbstractFaceElement(Context context) {
        mContext = context;
        mResources = mContext.getResources();
    }

    public void startSync( ScheduledExecutorService executorService ) {}
    public void stopSync( ) {}

    public int getColor( int colorId ) {
        return mResources.getColor( colorId, mContext.getTheme() );
    }

    public float getDimension( int dimensionId ) {
        return mResources.getDimension( dimensionId );
    }
    public void startActivity( Intent intent ) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(  intent );
    }

    public Bitmap getBitmap( int bitmapId ) {
        return BitmapFactory.decodeResource( mResources, bitmapId );
    }

    public Context getContext() { return mContext; }

    public void onAmbientModeChanged(boolean inAmbientMode, boolean lowBitAmbient) {
        mInAmbientMode = inAmbientMode;
        if (lowBitAmbient) {
            setAntiAlias( ! inAmbientMode );
        }
    }

    public boolean isInAmbientMode() { return mInAmbientMode; }

    public boolean isInInteractiveMode() { return !mInAmbientMode; }

    public abstract void setAntiAlias(boolean enabled);

    public abstract void onApplyWindowInsets(WindowInsets insets);

    public void drawTime( Canvas canvas, Calendar calendar, int x, int y) {}

    public void drawTime( Canvas canvas, Calendar calendar, FaceBoundComputer boundComputer, int x, int y) {
        drawTime( canvas, calendar, x, y );
    }

    protected void setTapBounds( Rect tapBounds) { mTapBounds = tapBounds; }

    public void onTapCommand(int tapType, int x, int y, long eventTime) {
        if ( mTapBounds != null ) {
            if ( tapType == WatchFaceService.TAP_TYPE_TAP && mTapBounds.contains( x, y ) ) {
                doTapCommand( tapType, x, y, eventTime );
            }
        }
    }

    public void doTapCommand( int tapType, int x, int y, long eventTime ) {}

    public void setInvalidateListener( InvalidateListener invalidateListener) {
        mInvalidateListener = invalidateListener;
    }

    public void postInvalidate() {
        if (mInvalidateListener != null) {
            mInvalidateListener.postInvalidate();
        }
    }

}
