package com.sebcano.bewiwatchface;

import com.sebcano.bewiwatchface.apiwrap.RectWrapper;

public class DimensionsComputer {
    // dimensions for 320 px height round with chin
    private static final int HEIGHT = 320;
    private static final int Y_WEATHER = 55;
    private static final int Y_DATE = 80;
    private static final int Y_TIME = 160;
    private static final int Y_BATTERY = 220;
    private static final int Y_MEETING = 260;

    private RectWrapper mBounds;

    public void setDimensions( RectWrapper bounds) {
        mBounds = bounds;
    }

    private int scaleY( int originalY ) {
        int originalHeight = HEIGHT;
        int currentHeight = mBounds.height();
        int centerY = originalHeight/2;
        int yOffset = originalY-centerY;
        int scaledOffset = yOffset * currentHeight / originalHeight;
        return currentHeight/2 + scaledOffset;
    }

    public int getYWeather() {
        return scaleY( Y_WEATHER );
    }

    public int getYDate() {
        return scaleY( Y_DATE );
    }

    public int getYTime() {
        return scaleY( Y_TIME );
    }

    public int getYBattery() {
        return scaleY( Y_BATTERY );
    }

    public int getYMeeting() {
        return scaleY( Y_MEETING );
    }
}
