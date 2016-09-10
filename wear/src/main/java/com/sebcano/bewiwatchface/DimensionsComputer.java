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

    private static final float WEATHER_HEIGHT = 19.968752f;
    private static final float DATE_HEIGHT = 33.28125f;
    private static final float TIME_HEIGHT = 113.15626f;
    private static final float BATTERY_HEIGHT = 19.968752f;
    private static final float SECONDS_HEIGHT = 33.28125f;
    private static final float CALENDAR_HEIGHT = 19.968752f;


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

    private float scaleTextSize( float originalSize ) {
        int originalHeight = HEIGHT;
        int currentHeight = mBounds.height();
        return originalSize * currentHeight / originalHeight;
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

    public float getWeatherHeight() { return scaleTextSize( WEATHER_HEIGHT ); }

    public float getDateHeight() { return scaleTextSize( DATE_HEIGHT ); }

    public float getTimeHeight() { return scaleTextSize( TIME_HEIGHT ); }

    public float getBatteryHeight() { return scaleTextSize( BATTERY_HEIGHT ); }

    public float getSecondsHeight() { return scaleTextSize( SECONDS_HEIGHT ); }

    public float getCalendarHeight() { return scaleTextSize( CALENDAR_HEIGHT ); }
}
