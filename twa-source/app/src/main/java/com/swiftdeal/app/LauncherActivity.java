package com.swiftdeal.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

/**
 * SwiftDeal TWA Launcher.
 *
 * Requests ACCESS_FINE_LOCATION in onCreate so Android shows the
 * "Allow SwiftDeal to access this device's location?" dialog immediately
 * on app launch. This is the version that produced the dialog in Jermane's
 * 9:28 PM screenshot.
 *
 * The dialog was closing too fast because index.html used to
 * window.location.replace('/welcome.html') on load, which reset Chrome's
 * WebView state and tore down the dialog. That redirect has been removed
 * and the welcome page content is now an in-page overlay inside index.html,
 * so no navigation happens and the dialog stays on screen until the user
 * responds.
 */
public class LauncherActivity
    extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1001);
            }
        }
    }

    @Override
    protected Uri getLaunchingUrl() {
        Uri uri = super.getLaunchingUrl();
        return uri;
    }
}
