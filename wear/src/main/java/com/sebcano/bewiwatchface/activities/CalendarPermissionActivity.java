package com.sebcano.bewiwatchface.activities;

import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;

public class CalendarPermissionActivity extends WearableActivity {

    private static final int PERMISSION_REQUEST_READ_CALENDAR = 1;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState);
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_CALENDAR},
                PERMISSION_REQUEST_READ_CALENDAR);
        finish();
    }
}
