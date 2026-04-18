package com.swiftdeal.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Dedicated permission request Activity.
 *
 * Why this exists:
 * The standard TWA LauncherActivity cannot reliably request runtime permissions because
 * it finishes itself within a few hundred milliseconds of launch to hand off to Chrome.
 * When it finishes, it tears down any permission dialog that was showing — that's why
 * the dialog was "flashing for 2 seconds" and disappearing before the user could respond.
 *
 * This Activity stays alive for as long as the user needs. It shows its own lightweight
 * screen with a "Continue" button that triggers the Android permission dialog. The
 * dialog is owned by THIS Activity (which doesn't finish), so Android keeps it open
 * until the user actually taps Allow / Deny / Only this time.
 *
 * Once the user responds (granted OR denied), we then launch the real TWA flow by
 * starting the TWA LauncherActivity.
 */
public class PermissionActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 1001;
    private static final String ASKED_KEY = "swiftdeal_location_asked_v1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we've already asked (or already granted), skip straight to TWA.
        boolean alreadyAsked = getSharedPreferences("swiftdeal", MODE_PRIVATE)
                .getBoolean(ASKED_KEY, false);
        boolean granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (alreadyAsked || granted) {
            launchTwaAndFinish();
            return;
        }

        // Build a simple programmatic UI — no XML layouts needed.
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

        TextView pin = new TextView(this);
        pin.setText("\uD83D\uDCCD"); // 📍
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
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(10);
        title.setLayoutParams(titleLp);
        root.addView(title);

        TextView msg = new TextView(this);
        msg.setText("SwiftDeal needs your precise location to find nearby drivers " +
                "and show your pickup point on the map.");
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
        allowBtn.setTypeface(null, android.graphics.Typeface.BOLD);
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
            @Override public void onClick(View v) { askForPermission(); }
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
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
        } else {
            launchTwaAndFinish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;

        // Mark as asked regardless of result — don't block the app if user denies.
        // Web code can still re-request through Chrome's delegation later.
        getSharedPreferences("swiftdeal", MODE_PRIVATE)
                .edit().putBoolean(ASKED_KEY, true).apply();

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            Toast.makeText(this,
                    "Location is needed to show the map. You can grant it later in Settings.",
                    Toast.LENGTH_LONG).show();
        }

        // Launch the TWA either way — user made their choice.
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
