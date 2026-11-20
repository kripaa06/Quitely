package com.example.quitely;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.quitely.fragments.DashboardFragment;
import com.example.quitely.fragments.HistoryFragment;
import com.example.quitely.fragments.SchedulesFragment;
import com.example.quitely.fragments.SettingsFragment;
import com.example.quitely.fragments.ZonesFragment;
import com.example.quitely.managers.GeofenceManager;
import com.example.quitely.notifications.NotificationHelper;
import com.example.quitely.utils.PermissionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment dashboardFragment = new DashboardFragment();
    private Fragment zonesFragment = new ZonesFragment();
    private Fragment schedulesFragment = new SchedulesFragment();
    private Fragment historyFragment = new HistoryFragment();
    private Fragment settingsFragment = new SettingsFragment();
    private Fragment activeFragment = dashboardFragment;
    private final FragmentManager fm = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createNotificationChannels(this);

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "5").hide(settingsFragment)
                    .add(R.id.fragment_container, historyFragment, "4").hide(historyFragment)
                    .add(R.id.fragment_container, schedulesFragment, "3").hide(schedulesFragment)
                    .add(R.id.fragment_container, zonesFragment, "2").hide(zonesFragment)
                    .add(R.id.fragment_container, dashboardFragment, "1")
                    .commit();
        } else {
            Fragment f1 = fm.findFragmentByTag("1");
            Fragment f2 = fm.findFragmentByTag("2");
            Fragment f3 = fm.findFragmentByTag("3");
            Fragment f4 = fm.findFragmentByTag("4");
            Fragment f5 = fm.findFragmentByTag("5");
            if (f1 != null) dashboardFragment = f1;
            if (f2 != null) zonesFragment = f2;
            if (f3 != null) schedulesFragment = f3;
            if (f4 != null) historyFragment = f4;
            if (f5 != null) settingsFragment = f5;

            if (settingsFragment != null && !settingsFragment.isHidden()) activeFragment = settingsFragment;
            else if (historyFragment != null && !historyFragment.isHidden()) activeFragment = historyFragment;
            else if (schedulesFragment != null && !schedulesFragment.isHidden()) activeFragment = schedulesFragment;
            else if (zonesFragment != null && !zonesFragment.isHidden()) activeFragment = zonesFragment;
            else activeFragment = dashboardFragment;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_dashboard) {
                switchFragment(dashboardFragment);
                return true;
            } else if (id == R.id.navigation_zones) {
                switchFragment(zonesFragment);
                return true;
            } else if (id == R.id.navigation_schedules) {
                switchFragment(schedulesFragment);
                return true;
            } else if (id == R.id.navigation_history) {
                switchFragment(historyFragment);
                return true;
            } else if (id == R.id.navigation_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });

        checkAndRequestInitialPermissions();
    }

    private void checkAndRequestInitialPermissions() {
        if (!PermissionManager.hasLocationPermission(this)) {
            PermissionManager.requestForegroundLocation(this);
        } else if (!PermissionManager.hasNotificationPermission(this)) {
            PermissionManager.requestNotificationPermission(this);
        } else if (!PermissionManager.hasBackgroundLocationPermission(this)) {
            promptBackgroundLocation();
        }
    }

    private void promptBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            new AlertDialog.Builder(this)
                    .setTitle("Background Silent Mode")
                    .setMessage("To automatically silence your phone even when the screen is off or Quitely is closed, select 'Allow all the time' in location settings.")
                    .setPositiveButton("Configure", (dialog, which) -> PermissionManager.requestBackgroundLocation(this))
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_LOCATION_FOREGROUND) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GeofenceManager.getInstance(this).checkAllZonesProximity();
                if (!PermissionManager.hasNotificationPermission(this)) {
                    PermissionManager.requestNotificationPermission(this);
                } else if (!PermissionManager.hasBackgroundLocationPermission(this)) {
                    promptBackgroundLocation();
                }
            }
        }
    }

    private void switchFragment(Fragment target) {
        if (activeFragment != target) {
            fm.beginTransaction().hide(activeFragment).show(target).commit();
            activeFragment = target;
        }
    }

    public void selectTab(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GeofenceManager.getInstance(this).startProximityMonitoring();
        GeofenceManager.getInstance(this).checkAllZonesProximity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GeofenceManager.getInstance(this).stopProximityMonitoring();
    }
}
