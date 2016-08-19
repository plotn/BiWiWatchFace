package sca.biwiwatchface.apiwrap;

import android.graphics.Rect;

public class ProductionRectWrapper implements RectWrapper {
    private Rect mRect;

    public void setRect( Rect rect ) {
        mRect = rect;
    }

    @Override
    public int left() { return mRect.left; }

    @Override
    public int right() { return mRect.right; }

    @Override
    public int bottom() {
        return mRect.bottom;
    }

    @Override
    public int width() { return mRect.width(); }

    @Override
    public int height() {
        return mRect.height();
    }

    @Override
    public int centerX() { return mRect.centerX(); }

    @Override
    public int centerY() { return mRect.centerY(); }


}
