package com.example.mychat;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS_NAME = "mychat_preferences";
    private static final String KEY_DARK_THEME = "dark_theme";

    /**
     * Initialize theme based on saved preference
     * Call this in MainActivity onCreate before super.onCreate()
     */
    public static void initializeTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkTheme = sharedPreferences.getBoolean(KEY_DARK_THEME, false);

        applyTheme(isDarkTheme);
    }

    /**
     * Apply theme to the entire app
     */
    public static void applyTheme(boolean isDarkTheme) {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Get current night mode setting
     */
    public static int getCurrentNightMode() {
        return AppCompatDelegate.getDefaultNightMode();
    }

    /**
     * Check if dark theme is currently enabled
     */
    public static boolean isDarkThemeEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false);
    }

    /**
     * Save theme preference
     */
    public static void saveThemePreference(Context context, boolean isDarkTheme) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(KEY_DARK_THEME, isDarkTheme)
                .apply();
    }

    /**
     * Toggle between light and dark theme
     */
    public static void toggleTheme(Context context) {
        boolean currentTheme = isDarkThemeEnabled(context);
        boolean newTheme = !currentTheme;

        saveThemePreference(context, newTheme);
        applyTheme(newTheme);
    }
}