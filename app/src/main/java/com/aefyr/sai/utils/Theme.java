package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.aefyr.sai.R;

import java.util.ArrayList;
import java.util.List;

public class Theme {
    private static final String THEME_TAG_CONCRETE = "concrete";
    private static final String THEME_TAG_LIGHT = "light";
    private static final String THEME_TAG_DARK = "dark";

    private static final int DEFAULT_LIGHT_THEME_ID = 0;
    private static final int DEFAULT_DARK_THEME_ID = 1;

    public enum Mode {
        /**
         * 使用单一选定主题
         */
        CONCRETE,
        /**
         * 根据系统主题自动切换亮/暗主题 (Android Q+)
         */
        AUTO_LIGHT_DARK
    }

    private static Theme sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<ThemeDescriptor> mThemes;
    private final MutableLiveData<ThemeDescriptor> mLiveTheme = new MutableLiveData<>();

    private Mode mMode;

    public static Theme getInstance(Context c) {
        synchronized (Theme.class) {
            return sInstance != null ? sInstance : new Theme(c);
        }
    }

    private Theme(Context c) {
        mContext = c.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mMode = Mode.valueOf(mPrefs.getString(PreferencesKeys.THEME_MODE,
                Utils.apiIsAtLeast(Build.VERSION_CODES.Q) ? Mode.AUTO_LIGHT_DARK.name() : Mode.CONCRETE.name()));

        mThemes = new ArrayList<>();
        mThemes.add(new ThemeDescriptor(0, R.style.AppTheme_Light, false, R.string.theme_sai_light));
        mThemes.add(new ThemeDescriptor(1, R.style.AppTheme_Dark, true, R.string.theme_sai_dark));

        invalidateLiveTheme();
        sInstance = this;
    }

    public static ThemeDescriptor apply(Context c) {
        Theme theme = getInstance(c);
        ThemeDescriptor currentTheme = theme.getCurrentTheme();
        c.setTheme(currentTheme.getTheme());
        theme.invalidateLiveTheme();
        return currentTheme;
    }

    public static void observe(Context c, @NonNull LifecycleOwner owner, @NonNull Observer<ThemeDescriptor> observer) {
        getInstance(c).getLiveTheme().observe(owner, observer);
    }

    public List<ThemeDescriptor> getThemes() {
        return mThemes;
    }

    public ThemeDescriptor getCurrentTheme() {
        return switch (mMode) {
            case CONCRETE -> getConcreteTheme();
            case AUTO_LIGHT_DARK -> shouldUseDarkThemeForAutoMode() ? getDarkTheme() : getLightTheme();
        };
    }

    public LiveData<ThemeDescriptor> getLiveTheme() {
        return mLiveTheme;
    }

    public Mode getThemeMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode == mMode) return;

        mPrefs.edit().putString(PreferencesKeys.THEME_MODE, mode.name()).apply();
        mMode = mode;
        invalidateLiveTheme();
    }

    public ThemeDescriptor getConcreteTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_CONCRETE, DEFAULT_LIGHT_THEME_ID));
    }

    public void setConcreteTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_CONCRETE, theme.getId());
        if (getThemeMode() == Mode.CONCRETE) invalidateLiveTheme();
    }

    public ThemeDescriptor getLightTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_LIGHT, DEFAULT_LIGHT_THEME_ID));
    }

    public void setLightTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_LIGHT, theme.getId());
        if (getThemeMode() == Mode.AUTO_LIGHT_DARK && !shouldUseDarkThemeForAutoMode()) {
            invalidateLiveTheme();
        }
    }

    public ThemeDescriptor getDarkTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_DARK, DEFAULT_DARK_THEME_ID));
    }

    public void setDarkTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_DARK, theme.getId());
        if (getThemeMode() == Mode.AUTO_LIGHT_DARK && shouldUseDarkThemeForAutoMode()) {
            invalidateLiveTheme();
        }
    }

    private boolean shouldUseDarkThemeForAutoMode() {
        return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private ThemeDescriptor getThemeDescriptorById(int themeId) {
        if (themeId == 1 || themeId == 2 || themeId == 4 || themeId == 7 || themeId == 8 || themeId == 10 || themeId == 11) {
            return mThemes.get(1);
        }
        return mThemes.get(0);
    }

    public static boolean isDynamicColorAvailable() {
        return com.google.android.material.color.DynamicColors.isDynamicColorAvailable();
    }

    public static boolean isDynamicColorEnabled(Context context) {
        return isDynamicColorAvailable() && PreferencesHelper.getInstance(context).isDynamicColorEnabled();
    }

    public static void setDynamicColorEnabled(Context context, boolean enabled) {
        PreferencesHelper.getInstance(context).setDynamicColorEnabled(enabled);
        getInstance(context).invalidateLiveTheme();
    }

    private int getThemeId(String themeTag, int defaultThemeId) {
        return mPrefs.getInt(PreferencesKeys.CURRENT_THEME + "." + themeTag, defaultThemeId);
    }

    private void saveThemeId(String themeTag, int themeId) {
        mPrefs.edit().putInt(PreferencesKeys.CURRENT_THEME + "." + themeTag, themeId).apply();
    }

    public void invalidateLiveTheme() {
        ThemeDescriptor currentTheme = getCurrentTheme();
        if (!currentTheme.equals(mLiveTheme.getValue())) {
            mLiveTheme.setValue(currentTheme);
        }
    }

    public static class ThemeDescriptor {
        private final int mId;
        private final int mTheme;
        private final boolean mIsDark;
        private final int mNameStringRes;

        private ThemeDescriptor(int id, @StyleRes int theme, boolean isDark, @StringRes int nameStringRes) {
            mId = id;
            mTheme = theme;
            mIsDark = isDark;
            mNameStringRes = nameStringRes;
        }

        public int getId() {
            return mId;
        }

        @StyleRes
        public int getTheme() {
            return mTheme;
        }

        public boolean isDark() {
            return mIsDark;
        }

        public String getName(Context c) {
            return c.getString(mNameStringRes);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ThemeDescriptor && ((ThemeDescriptor) obj).getId() == getId();
        }

        @Override
        public int hashCode() {
            return mId;
        }
    }
}