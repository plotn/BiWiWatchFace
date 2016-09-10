package com.sebcano.bewiwatchface.data;

public class Meeting {
    private long mBeginDate;
    private String mBeginDateString;
    private String mTitle;

    public Meeting( long beginDate, String beginDateString, String title ) {
        mBeginDate = beginDate;
        mBeginDateString = beginDateString;
        mTitle = title;
    }

    @Override
    public boolean equals( Object o ) {
        if ( ! (o instanceof Meeting) ) return false;
        Meeting otherMeeting = (Meeting) o;
        return mBeginDate == otherMeeting.mBeginDate
                && mTitle.equals( otherMeeting.mTitle );
    }

    public long getBeginDate() {
        return mBeginDate;
    }

    public String getBeginDateString() {
        return mBeginDateString;
    }

    public String getTitle() {
        return mTitle;
    }

}
