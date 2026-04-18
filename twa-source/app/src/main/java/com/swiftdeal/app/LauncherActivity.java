package com.swiftdeal.app;

import android.os.Bundle;

/**
 * Standard TWA launcher. Permission requests are handled by PermissionActivity,
 * which runs BEFORE this Activity and starts it only after the user has
 * responded to the location permission dialog.
 *
 * Do NOT add requestPermissions() calls here — this Activity finishes itself
 * within a few hundred milliseconds of launch to hand off to Chrome, and any
 * permission dialog owned by this Activity gets torn down when it finishes.
 * That's the bug we just fixed.
 */
public class LauncherActivity
        extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
