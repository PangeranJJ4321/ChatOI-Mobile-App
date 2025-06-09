package com.example.mychat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable; // Tambah import

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_NEW_CONVERSATION = 1; // Define a request code

    // BroadcastReceiver untuk menerima total unread count dari ConversationFragment
    private BroadcastReceiver totalUnreadCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int totalUnreadCount = intent.getIntExtra("total_unread_count", 0);
            Log.d(TAG, "Received total_unread_count: " + totalUnreadCount);
            updateBadge(totalUnreadCount);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme BEFORE super.onCreate()
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Buka fragment pertama saat pertama kali activity dibuat
        if (savedInstanceState == null) {
            replaceFragment(new ConversationFragment());
        }

        // Listener buat nav bar bawah
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.conversationsFragment) {
                selectedFragment = new ConversationFragment();
            } else if (itemId == R.id.profileFragment) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.settingsFragment) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                return true;
            }
            return false;
        });

        ImageButton btnNewChat = findViewById(R.id.btn_new_chat);
        btnNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewConversationActivity.class);
            startActivityForResult(intent, REQUEST_CODE_NEW_CONVERSATION);
        });

        View bottomNav = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        // Daftarkan BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                totalUnreadCountReceiver, new IntentFilter("com.example.mychat.UPDATE_TOTAL_UNREAD_COUNT")
        );
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // Metode untuk mengupdate badge di BottomNavigationView
    private void updateBadge(int count) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.conversationsFragment);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
            // Opsional: atur warna, posisi, dll.
            // badge.setBackgroundColor(ContextCompat.getColor(this, R.color.badge_background));
            // badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.badge_text));
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NEW_CONVERSATION && resultCode == RESULT_OK) {
            // Ketika percakapan baru dibuat, refresh ConversationFragment
            // Secara ideal, ConversationFragment akan merespons event Pusher (conversation-updated)
            // yang dipicu backend saat percakapan baru dibuat.
            // Panggilan manual onResume() pada fragment umumnya tidak disarankan
            // karena siklus hidup fragment. fetchUserConversations() akan dipanggil di onResume() fragment.
            // Jadi, bagian ini mungkin tidak perlu diubah lebih lanjut.
            // Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            // if (currentFragment instanceof ConversationFragment) {
            //     ((ConversationFragment) currentFragment).onResume();
            // }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan pendaftaran BroadcastReceiver saat Activity dihancurkan
        LocalBroadcastManager.getInstance(this).unregisterReceiver(totalUnreadCountReceiver);
    }
}