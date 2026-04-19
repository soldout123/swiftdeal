package com.swiftdeal.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

/**
 * SwiftDeal TWA Launcher.
 *
 * Uses the OFFICIAL Google Android Browser Helper async-launch pattern:
 *
 *   1. Override shouldLaunchImmediately() to return false when we need to
 *      perform an async task (here: wait for location permission) BEFORE
 *      launching the TWA.
 *   2. The parent's onCreate() then does everything except launch Chrome.
 *      This Activity stays alive, the permission dialog is shown, and since
 *      we don't finish, the dialog persists on screen until the user taps.
 *   3. In onRequestPermissionsResult(), we call launchTwa() which hands off
 *      to Chrome.
 *
 * This is the pattern Google themselves document for apps that need to do
 * something asynchronously before launching the TWA (like fetching a Firebase
 * token or, in our case, getting a permission grant).
 *
 * Reference: https://developer.chrome.com/docs/android/trusted-web-activity/offline-first
 */
public class LauncherActivity
    extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    private static final int REQ_LOCATION = 1001;
    private boolean mLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If permission is already granted (or pre-M Android), launch immediately.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            if (!mLaunched) {
                mLaunched = true;
                launchTwa();
            }
            return;
        }

        // Permission not granted yet. shouldLaunchImmediately() returned false,
        // so the parent did NOT call launchTwa(). We now ask for permission.
        // This Activity stays alive owning the dialog. The dialog will remain
        // on screen until the user responds.
        requestPermissions(new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQ_LOCATION);
    }

    @Override
    protected boolean shouldLaunchImmediately() {
        // Return true only if we already have the permission OR on pre-M Android.
        // Return false if we need to ask for permission first — this keeps the
        // parent from calling launchTwa() automatically, so this Activity stays
        // alive to own the permission dialog.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;
        // User has responded (granted or denied). Either way, launch the TWA now.
        // If granted, Chrome's location delegation will work immediately.
        // If denied, the web app still works — the user can re-grant via the
        // in-app Settings toggle (which opens phone Settings).
        if (!mLaunched) {
            mLaunched = true;
            launchTwa();
        }
    }

    @Override
    protected Uri getLaunchingUrl() {
        Uri uri = super.getLaunchingUrl();
        return uri;
    }
}
