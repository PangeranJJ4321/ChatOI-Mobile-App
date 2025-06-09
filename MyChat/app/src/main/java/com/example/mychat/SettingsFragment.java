package com.example.mychat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private SwitchCompat switchTheme;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "mychat_preferences";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize views
        initViews(view);

        // Setup theme switch
        setupThemeSwitch();
    }

    private void initViews(View view) {
        switchTheme = view.findViewById(R.id.switch_theme);
    }

    private void setupThemeSwitch() {
        // Get current theme preference
        boolean isDarkTheme = sharedPreferences.getBoolean(KEY_DARK_THEME, false);

        // Set switch state based on current theme
        switchTheme.setChecked(isDarkTheme);

        // Set switch listener
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            ThemeManager.saveThemePreference(requireContext(), isChecked);

            // Apply theme
            ThemeManager.applyTheme(isChecked);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update switch state when fragment resumes
        if (switchTheme != null) {
            boolean isDarkTheme = sharedPreferences.getBoolean(KEY_DARK_THEME, false);
            switchTheme.setChecked(isDarkTheme);
        }
    }
}