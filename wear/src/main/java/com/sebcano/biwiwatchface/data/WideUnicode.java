package com.sebcano.biwiwatchface.data;

public class WideUnicode {
    public static String toString( int unicodeChar) {
        return new String( Character.toChars(unicodeChar) );
    }
}
