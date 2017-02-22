package com.xdja.zdsb.view;

import com.xdja.zdsb.utils.Zzlog;

import android.content.Intent;
import android.os.Bundle;

public class DriverLicenseActivity extends IDCardRecognizeActivity {

    private static final String TAG = "DriverLicenseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Zzlog.out(TAG, "onCreate(Bundle savedInstanceState)");
        Intent intent = getIntent();
        // 5
        intent.putExtra("nMainId", 5);
    }
}