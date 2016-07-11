package sca.biwiwatchface;

import sca.biwiwatchface.apiwrap.RectWrapper;

public class FaceBoundComputer {

    private RectWrapper mBounds;
    private boolean mIsRound;

    void setDimensions( RectWrapper bounds, boolean isRound ) {
        mBounds = bounds;
        mIsRound = isRound;
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
    

}
