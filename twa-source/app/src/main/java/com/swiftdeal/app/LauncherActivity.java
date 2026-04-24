package com.swiftdeal.app;

import android.net.Uri;
import android.os.Bundle;

/**
 * SwiftDeal TWA Launcher — NATIVE-SIMPLE version.
 *
 * Why this is simple:
 * Previous versions called requestPermissions() from onCreate() which showed
 * the native location popup BEFORE Chrome loaded, BEFORE the welcome page
 * rendered — i.e. "before the app opens" from the user's perspective.
 *
 * The Android permission (ACCESS_FINE_LOCATION) is declared in the manifest,
 * which is all that's needed at the APK level. The actual permission grant is
 * triggered from the web side via navigator.geolocation.getCurrentPosition(),
 * which Chrome's Location Delegation (LOCATION_DELEGATION_ENABLED=true) routes
 * through to the Android OS prompt — on top of the fully-loaded app.
 *
 * Result: user sees the welcome page first, dismisses it, THEN gets the
 * permission prompt — making it clear which app is asking and why.
 */
public class LauncherActivity
    extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Intentionally NO requestPermissions() here.
        // The web app triggers navigator.geolocation.getCurrentPosition() after
        // the welcome page is dismissed, and Chrome delegates that to Android's
        // native permission prompt — appearing on top of the loaded app.
    }

    @Override
    protected Uri getLaunchingUrl() {
        return super.getLaunchingUrl();
    }
}
