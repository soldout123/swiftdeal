package com.swiftdeal.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dedicated permission request Activity.
 *
 * Why this exists:
 * TWA LauncherActivity finishes itself within a few hundred milliseconds of launch
 * to hand off to Chrome. When it finishes, it tears down any permission dialog
 * it was showing — that's why the dialog was "flashing for 2 seconds" and
 * disappearing before the user could respond.
 *
 * This Activity stays alive for as long as the user needs. It shows its own simple
 * screen with a "GRANT PRECISE LOCATION" button. Tapping it triggers the Android
 * permission dialog, which is owned by THIS Activity (which doesn't finish) — so
 * Android keeps the dialog open until the user responds.
 *
 * Once the user responds (granted OR denied), this Activity starts LauncherActivity
 * and finishes itself. On subsequent app launches, it sees permission is already
 * granted (or already asked) and skips straight to LauncherActivity instantly.
 *
 * Uses plain android.app.Activity — no AppCompat dependency required.
 */
public class PermissionActivity extends Activity {

    private static final int REQ_LOCATION = 1001;
    private static final String PREFS = "swiftdeal";
    private static final String ASKED_KEY = "location_asked_v1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip straight to TWA if already granted, or if we've already asked once
        // (user can re-request via in-app Settings → App Permissions later).
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean alreadyAsked = prefs.getBoolean(ASKED_KEY, false);
        boolean granted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED);

        if (granted || alreadyAsked) {
            launchTwaAndFinish();
            return;
        }

        setContentView(buildUi());
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.parseColor("#0A0A0A"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 📍 emoji icon
        TextView pin = new TextView(this);
        pin.setText("\uD83D\uDCCD");
        pin.setTextSize(56);
        pin.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pinLp.bottomMargin = dp(12);
        pin.setLayoutParams(pinLp);
        root.addView(pin);

        TextView title = new TextView(this);
        title.setText("Location Required");
        title.setTextColor(Color.parseColor("#F9D71C"));
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(10);
        title.setLayoutParams(titleLp);
        root.addView(title);

        TextView msg = new TextView(this);
        msg.setText("SwiftDeal needs your precise location to find nearby drivers "
                + "and show your pickup point on the map.");
        msg.setTextColor(Color.parseColor("#CCCCCC"));
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.bottomMargin = dp(24);
        msgLp.leftMargin = dp(8);
        msgLp.rightMargin = dp(8);
        msg.setLayoutParams(msgLp);
        root.addView(msg);

        Button allowBtn = new Button(this);
        allowBtn.setText("GRANT PRECISE LOCATION");
        allowBtn.setTextColor(Color.BLACK);
        allowBtn.setTextSize(15);
        allowBtn.setTypeface(null, Typeface.BOLD);
        allowBtn.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F9D71C"));
        bg.setCornerRadius(dp(14));
        allowBtn.setBackground(bg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        btnLp.bottomMargin = dp(12);
        allowBtn.setLayoutParams(btnLp);
        allowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { askForPermission(); }
        });
        root.addView(allowBtn);

        TextView hint = new TextView(this);
        hint.setText("Select \"Precise\" and \"While using the app\"");
        hint.setTextColor(Color.parseColor("#888888"));
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hint.setLayoutParams(hintLp);
        root.addView(hint);

        return root;
    }

    private void askForPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
        } else {
            launchTwaAndFinish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean(ASKED_KEY, true).apply();

        boolean granted = grantResults != null
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            Toast.makeText(this,
                    "Location is needed to show the map. You can enable it later in Settings.",
                    Toast.LENGTH_LONG).show();
        }

        launchTwaAndFinish();
    }

    private void launchTwaAndFinish() {
        Intent i = new Intent(this, LauncherActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
