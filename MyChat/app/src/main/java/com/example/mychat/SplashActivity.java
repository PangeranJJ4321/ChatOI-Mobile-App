// SplashActivity.java
package com.example.mychat;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mychat.ui.viewmodel.AuthViewModel;
import com.example.mychat.ui.viewmodel.ViewModelFactory;


public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private AuthViewModel authViewModel;
    private static final long SPLASH_DISPLAY_LENGTH = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.initializeTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authViewModel = new ViewModelProvider(this, new ViewModelFactory(getApplication())).get(AuthViewModel.class);

        // Splash tampil dulu 4 detik, baru mulai cek auth
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Splash delay done. Checking initial auth state...");

            if (!isNetworkAvailable()) {
                Log.d(TAG, "No network connection.");
                Toast.makeText(SplashActivity.this, "No internet connection. Some features may be limited.", Toast.LENGTH_LONG).show();
            }

            // Mulai observe setelah delay
            observeAuthState();
            authViewModel.checkInitialAuthState();

        }, SPLASH_DISPLAY_LENGTH);
    }

    private void observeAuthState() {
        authViewModel.isAuthenticated.observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                Log.d(TAG, "User is authenticated. Redirecting to MainActivity.");
                redirectToMain();
            } else {
                Log.d(TAG, "User is NOT authenticated. Redirecting to LoginActivity.");
                redirectToLogin();
            }
        });

        authViewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Log.e(TAG, "Auth error from ViewModel: " + message);
            }
        });
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void redirectToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}