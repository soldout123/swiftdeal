package com.swiftdeal.app;

import android.os.Bundle;

public class LauncherActivity
        extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Permission requests are handled by DelegationService + Chrome TWA
           bridge — not here. Requesting them in the launcher Activity causes
           the native dialog to be destroyed along with the Activity when it
           hands off to the browser, which is why it "flashed for 2 seconds". */
    }
}
