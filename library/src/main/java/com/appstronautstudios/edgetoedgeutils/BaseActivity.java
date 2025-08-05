package com.appstronautstudios.edgetoedgeutils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        int colorPrimary = getThemeColor(this, androidx.appcompat.R.attr.colorPrimary);
        setStatusBarColor(colorPrimary);
        setStatusBarScrimColor(colorPrimary);
        setNavigationBarScrimColor(colorPrimary);

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void setContentView(int layoutResID) {
        View root = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        FrameLayout container = root.findViewById(R.id.content_container);
        LayoutInflater.from(this).inflate(layoutResID, container, true);
        super.setContentView(root);

        applyInsets(root);
        root.post(() -> ViewCompat.requestApplyInsets(root));
    }

    private void applyInsets(View root) {
        final View statusScrim = root.findViewById(R.id.statusBarScrim);
        final View navScrim = root.findViewById(R.id.navigationBarScrim);
        final View content = root.findViewById(R.id.content_container);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            final Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            final boolean imeVisible = ime.bottom > 0;

            // --- Top scrim reserves status bar space ---
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }

            // --- Bottom scrim reserves nav bar space when IME hidden ---
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = imeVisible ? 0 : sys.bottom; // hide when keyboard shown
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility((!imeVisible && sys.bottom > 0) ? View.VISIBLE : View.GONE);
            }

            // --- Content padding: no top (scrim already reserves it); bottom = IME when shown ---
            if (content != null) {
                int left = sys.left;                  // for cutouts/gestures
                int top = 0;                         // scrim handles status area
                int right = sys.right;
                int bottom = imeVisible ? ime.bottom : 0; // nav scrim handles nav when IME hidden
                content.setPadding(left, top, right, bottom);
            }

            // We handled everything here; don't let children double-apply.
            return WindowInsetsCompat.CONSUMED;
        });

        // Make sure insets flow after the view is attached
        root.post(() -> ViewCompat.requestApplyInsets(root));
    }

    public void setStatusBarScrimColor(@ColorInt int color) {
        View statusScrim = findViewById(R.id.statusBarScrim);
        if (statusScrim != null) {
            statusScrim.setBackgroundColor(color);
        }
    }

    public void setNavigationBarScrimColor(@ColorInt int color) {
        View navScrim = findViewById(R.id.navigationBarScrim);
        if (navScrim != null) {
            navScrim.setBackgroundColor(color);
        }
    }

    public void setStatusBarColor(@ColorInt int color) {
        getWindow().setStatusBarColor(color);
        setLightStatusBarIcons(shouldUseDarkIcons(color));
    }

    private void setLightStatusBarIcons(boolean useDarkIcons) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(useDarkIcons); // true = dark icons
    }

    private boolean shouldUseDarkIcons(@ColorInt int color) {
        // Standard luminance calculation
        int brightness = (int) ((Color.red(color) * 0.299) +
                (Color.green(color) * 0.587) +
                (Color.blue(color) * 0.114));
        return brightness > 160;  // use dark icons on light backgrounds
    }

    private static int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        if (theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(context, typedValue.resourceId);
            } else {
                return typedValue.data; // raw color value
            }
        }
        throw new IllegalArgumentException("Attribute not found");
    }
}