package sca.biwiwatchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;

public abstract class AbstractFaceElement {
    private Context mContext;
    private Resources mResources;
    private boolean mInAmbientMode;

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

    public Bitmap getBitmap( int bitmapId ) {
        return BitmapFactory.decodeResource( mResources, bitmapId );
    }

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

}
