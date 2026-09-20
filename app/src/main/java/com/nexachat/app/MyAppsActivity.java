package com.nexachat.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.adapters.InstalledAppsAdapter;
import com.nexachat.app.databinding.ActivityMyAppsBinding;
import com.nexachat.app.models.InstalledAppInfo;
import com.nexachat.app.security.SecurityHelper;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyAppsActivity extends AppCompatActivity {

    private static final String PREFS_MY_APPS = "nexachat_my_apps_prefs";
    private static final String KEY_PACKAGES = "saved_packages";

    private ActivityMyAppsBinding binding;
    private InstalledAppsAdapter adapter;
    private final List<InstalledAppInfo> savedAppsList = new ArrayList<>();
    private SharedPreferences myAppsPrefs;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyAppsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        packageManager = getPackageManager();
        myAppsPrefs = getSharedPreferences(PREFS_MY_APPS, Context.MODE_PRIVATE);

        setupUI();
        loadSavedApps();
    }

    private void setupUI() {
        binding.btnMyAppsBack.setOnClickListener(v -> finish());

        adapter = new InstalledAppsAdapter(this, new InstalledAppsAdapter.OnAppActionListener() {
            @Override
            public void onAppClick(InstalledAppInfo appInfo) {
                // Check if App Verification is enabled globally or if app is secured
                if (SecurityHelper.isAppVerificationEnabled(MyAppsActivity.this) || appInfo.isSecured()) {
                    performVerificationAndLaunch(appInfo);
                } else {
                    launchApplication(appInfo.getPackageName());
                }
            }

            @Override
            public void onAppLongClick(InstalledAppInfo appInfo) {
                // Long press always triggers NexaChat security verification
                performVerificationAndLaunch(appInfo);
            }

            @Override
            public void onAppRemove(InstalledAppInfo appInfo) {
                removeApp(appInfo);
            }
        });

        binding.rvMyApps.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyApps.setAdapter(adapter);

        binding.btnAddApp.setOnClickListener(v -> showAddAppChooserDialog());
        binding.btnEmptyAddApp.setOnClickListener(v -> showAddAppChooserDialog());
    }

    private void loadSavedApps() {
        savedAppsList.clear();
        Set<String> savedPackages = myAppsPrefs.getStringSet(KEY_PACKAGES, new HashSet<>());

        if (savedPackages != null) {
            for (String pkg : savedPackages) {
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(pkg, 0);
                    String label = packageManager.getApplicationLabel(appInfo).toString();
                    savedAppsList.add(new InstalledAppInfo(label, pkg, true));
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        adapter.setApps(savedAppsList);
        binding.emptyAppsLayout.setVisibility(savedAppsList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveAppsList() {
        Set<String> pkgSet = new HashSet<>();
        for (InstalledAppInfo app : savedAppsList) {
            pkgSet.add(app.getPackageName());
        }
        myAppsPrefs.edit().putStringSet(KEY_PACKAGES, pkgSet).apply();
    }

    private void showAddAppChooserDialog() {
        // Query all installed launchable apps
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);

        List<String> names = new ArrayList<>();
        List<String> packages = new ArrayList<>();

        for (ResolveInfo ri : resolveInfos) {
            String pkg = ri.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) {
                String label = ri.loadLabel(packageManager).toString();
                names.add(label);
                packages.add(pkg);
            }
        }

        CharSequence[] items = names.toArray(new CharSequence[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select App / Game to Add")
                .setItems(items, (dialog, which) -> {
                    String selectedPkg = packages.get(which);
                    String selectedName = names.get(which);

                    // Check if already in list
                    for (InstalledAppInfo a : savedAppsList) {
                        if (a.getPackageName().equals(selectedPkg)) {
                            Toast.makeText(this, "App already in your launcher", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    InstalledAppInfo newApp = new InstalledAppInfo(selectedName, selectedPkg, true);
                    savedAppsList.add(newApp);
                    saveAppsList();
                    adapter.setApps(savedAppsList);
                    binding.emptyAppsLayout.setVisibility(View.GONE);
                    Toast.makeText(this, selectedName + " added with NexaChat Protection", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performVerificationAndLaunch(InstalledAppInfo appInfo) {
        SecurityHelper.authenticate(this, "Security Verification",
                "Authenticate with NexaChat to open " + appInfo.getAppName(),
                new SecurityHelper.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MyAppsActivity.this, "Access verified for " + appInfo.getAppName(), Toast.LENGTH_SHORT).show();
                        launchApplication(appInfo.getPackageName());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MyAppsActivity.this, "Verification failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchApplication(String packageName) {
        try {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Unable to launch this app", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error launching application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeApp(InstalledAppInfo appInfo) {
        savedAppsList.remove(appInfo);
        saveAppsList();
        adapter.setApps(savedAppsList);
        binding.emptyAppsLayout.setVisibility(savedAppsList.isEmpty() ? View.VISIBLE : View.GONE);
        Toast.makeText(this, appInfo.getAppName() + " removed", Toast.LENGTH_SHORT).show();
    }
}
