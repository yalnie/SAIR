package com.aefyr.sai.ui.activities;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.utils.Theme;
import com.google.android.material.color.DynamicColors;

@SuppressLint("Registered") //This is only a base class for other activities
public class ThemedActivity extends AppCompatActivity {

    private Theme.ThemeDescriptor mAppliedTheme;
    private boolean mAppliedDynamicColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mAppliedTheme = Theme.apply(this);
        mAppliedDynamicColor = Theme.isDynamicColorEnabled(this);
        if (mAppliedDynamicColor) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        Theme.observe(this, this, theme -> {
            if (!theme.equals(mAppliedTheme))
                recreate();
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAppliedDynamicColor != Theme.isDynamicColorEnabled(this)) {
            recreate();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
