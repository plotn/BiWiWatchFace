package com.sebcano.biwiwatchface.data;

public class Meeting {
    private String mBeginDate;

    private String mTitle;

    public Meeting( String beginDate, String title ) {
        mBeginDate = beginDate;
        mTitle = title;
    }

    @Override
    public boolean equals( Object o ) {
        if ( ! (o instanceof Meeting) ) return false;
        Meeting otherMeeting = (Meeting) o;
        return mBeginDate.equals( otherMeeting.mBeginDate )
                && mTitle.equals( otherMeeting.mTitle );
    }

    public String getBeginDate() {
        return mBeginDate;
    }

    public String getTitle() {
        return mTitle;
    }

}
