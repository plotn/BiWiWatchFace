package com.sebcano.bewiwatchface;

import com.sebcano.bewiwatchface.apiwrap.RectWrapper;

public class FaceBoundComputer {

    private RectWrapper mBounds;
    private boolean mIsRound;
    private int mChinSize;

    void setDimensions( RectWrapper bounds, boolean isRound, int chinSize ) {
        mBounds = bounds;
        mIsRound = isRound;
        mChinSize = chinSize;
    }

    int getLeftSide( int y ) {
        if (! mIsRound) {
            return mBounds.left();
        } else {
            // x^2 = r^2 - y^2, for x,y based on circle center
            int ySide = mBounds.centerY()-y;
            int y2 = ySide*ySide;
            int r2 = mBounds.width()*mBounds.width()/4;
            int x2 = r2 - y2;
            int xSide = (int) Math.floor( Math.sqrt( x2 ) );
            return mBounds.centerX() - xSide;
        }
    }

    int getRightSide( int y ) {
        int xLeft = getLeftSide( y );
        int leftOffset = xLeft - mBounds.left();
        return mBounds.right() - leftOffset;
    }

    boolean isYInScreen( int y ) {
        return y < mBounds.bottom()-mChinSize;
    }
    

}
