package com.sebcano.biwiwatchface;

import com.sebcano.biwiwatchface.apiwrap.RectWrapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

class FakeRect implements RectWrapper {

    private int left;
    private int top;
    private int right;
    private int bottom;

    FakeRect( int l, int t, int r, int b) {
        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    @Override
    public int left() {
        return left;
    }

    @Override
    public int right() {
        return right;
    }

    @Override
    public int bottom() { return bottom; }

    @Override
    public int width() {
        return right-left;
    }

    @Override
    public int height() {
        return bottom-top;
    }

    @Override
    public int centerX() {
        return (left + right) >> 1;
    }

    @Override
    public int centerY() {
        return (top + bottom) >> 1;
    }
}

public class FaceBoundComputerTest {

    private final static double COS_30 = 0.86602540378443864676372317075294;
    private final static double SIN_60 = COS_30;

    @Test
    public void should_compute_left_side_of_round_watch() throws Exception {
        RectWrapper bounds = new FakeRect(0, 0, 100, 100);
        FaceBoundComputer l = new FaceBoundComputer();
        l.setDimensions( bounds, true, 0 );
        assertEquals(50, l.getLeftSide( 0 ));
        assertEquals(0, l.getLeftSide( 50 ));
        assertEquals(50, l.getLeftSide( 100 ));
        assertEquals(Math.ceil((1-COS_30)*50), l.getLeftSide( 25 ), 0);
        assertEquals( 25, l.getLeftSide( (int) (50 + SIN_60*50 ) ) );
    }
}