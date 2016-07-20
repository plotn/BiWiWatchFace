package sca.biwiwatchface;

public class CachedStringFromLong {
    public interface StringFormatter {
        String computeString(long value);
    }

    private long mLastValue;
    private String mCachedString;
    private StringFormatter mFormatter;

    public CachedStringFromLong( StringFormatter formatter ) {
        mFormatter = formatter;
    }

    public String getString(long value) {
        if (mCachedString == null || mLastValue != value) {
            mCachedString = mFormatter.computeString( value );
            mLastValue = value;
        }
        return mCachedString;
    }
}