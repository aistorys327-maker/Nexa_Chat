package com.nexachat.app.security;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.nexachat.app.CamouflageGatewayActivity;
import com.nexachat.app.R;

import java.util.Collections;

public class DisguiseManager {

    private static final String PREF_NAME = "nexachat_camouflage_prefs";
    private static final String KEY_DISGUISE_ENABLED = "disguise_enabled";
    private static final String KEY_DISGUISE_TYPE = "disguise_type";
    private static final String KEY_DISGUISED_APP_NAME = "disguised_app_name";
    private static final String KEY_DISGUISED_PACKAGE = "disguised_package";
    private static final String KEY_ACTIVE_ALIAS = "active_alias";

    public static final String TYPE_NONE = "none";
    public static final String TYPE_CALCULATOR = "calculator";
    public static final String TYPE_NOTES = "notes";
    public static final String TYPE_CLOCK = "clock";
    public static final String TYPE_GAME = "game";
    public static final String TYPE_CAMERA = "camera";
    public static final String TYPE_WEATHER = "weather";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_CUSTOM = "custom";

    public static final String ALIAS_DEFAULT = "com.nexachat.app.LauncherDefault";
    public static final String ALIAS_CALCULATOR = "com.nexachat.app.LauncherAliasCalculator";
    public static final String ALIAS_NOTES = "com.nexachat.app.LauncherAliasNotes";
    public static final String ALIAS_CLOCK = "com.nexachat.app.LauncherAliasClock";
    public static final String ALIAS_GAME = "com.nexachat.app.LauncherAliasGame";
    public static final String ALIAS_CAMERA = "com.nexachat.app.LauncherAliasCamera";
    public static final String ALIAS_WEATHER = "com.nexachat.app.LauncherAliasWeather";
    public static final String ALIAS_MUSIC = "com.nexachat.app.LauncherAliasMusic";
    public static final String ALIAS_CUSTOM = "com.nexachat.app.LauncherAliasCustom";

    private static DisguiseManager instance;

    private DisguiseManager() {}

    public static synchronized DisguiseManager getInstance() {
        if (instance == null) {
            instance = new DisguiseManager();
        }
        return instance;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDisguiseEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DISGUISE_ENABLED, false);
    }

    public String getDisguiseType(Context context) {
        return getPrefs(context).getString(KEY_DISGUISE_TYPE, TYPE_NONE);
    }

    public String getDisguisedAppName(Context context) {
        return getPrefs(context).getString(KEY_DISGUISED_APP_NAME, "Calculator");
    }

    public String getDisguisedPackageName(Context context) {
        return getPrefs(context).getString(KEY_DISGUISED_PACKAGE, "");
    }

    public String getActiveAlias(Context context) {
        return getPrefs(context).getString(KEY_ACTIVE_ALIAS, "");
    }

    /**
     * Activates app camouflage:
     * Disguises NexaChat's launcher icon & label on device home screen/app drawer,
     * and sets up the stealth trigger.
     */
    public void enableDisguise(Context context, String type, String appName, String packageName, String aliasClassName) {
        PackageManager pm = context.getPackageManager();

        // Save preferences
        getPrefs(context).edit()
                .putBoolean(KEY_DISGUISE_ENABLED, true)
                .putString(KEY_DISGUISE_TYPE, type)
                .putString(KEY_DISGUISED_APP_NAME, appName)
                .putString(KEY_DISGUISED_PACKAGE, packageName != null ? packageName : "")
                .putString(KEY_ACTIVE_ALIAS, aliasClassName)
                .apply();

        // 1. Enable chosen alias and disable all other aliases
        String[] allAliases = new String[]{
                ALIAS_CALCULATOR, ALIAS_NOTES, ALIAS_CLOCK, ALIAS_GAME,
                ALIAS_CAMERA, ALIAS_WEATHER, ALIAS_MUSIC, ALIAS_CUSTOM
        };

        for (String alias : allAliases) {
            int state = (aliasClassName != null && alias.equals(aliasClassName))
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            try {
                pm.setComponentEnabledSetting(
                        new ComponentName(context, alias),
                        state,
                        PackageManager.DONT_KILL_APP
                );
            } catch (Throwable ignored) {}
        }

        // 2. Disable default launcher alias so only the selected disguised icon appears on mobile screen
        try {
            pm.setComponentEnabledSetting(
                    new ComponentName(context, ALIAS_DEFAULT),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {}

        // 3. Keep MainActivity component itself enabled for internal navigations
        try {
            pm.setComponentEnabledSetting(
                    new ComponentName(context, "com.nexachat.app.MainActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {}

        // 3. Register Home Screen Long-Press Launcher Shortcut for direct mobile Face Unlock
        registerLauncherShortcut(context);
    }

    private void registerLauncherShortcut(Context context) {
        try {
            Intent unlockIntent = new Intent(context, CamouflageGatewayActivity.class);
            unlockIntent.setAction(Intent.ACTION_VIEW);
            unlockIntent.putExtra("direct_face_unlock", true);
            unlockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, "shortcut_face_unlock")
                    .setShortLabel("Open")
                    .setLongLabel("Instant Face Unlock")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_lock))
                    .setIntent(unlockIntent)
                    .build();

            ShortcutManagerCompat.setDynamicShortcuts(context, Collections.singletonList(shortcut));
        } catch (Throwable ignored) {}
    }

    /**
     * Disables disguise mode and restores default NexaChat icon and name on home screen.
     */
    public void disableDisguise(Context context) {
        PackageManager pm = context.getPackageManager();

        // 1. Enable standard MainActivity and default launcher alias
        try {
            pm.setComponentEnabledSetting(
                    new ComponentName(context, "com.nexachat.app.MainActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {}

        try {
            pm.setComponentEnabledSetting(
                    new ComponentName(context, ALIAS_DEFAULT),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {}

        // 2. Disable all disguise aliases
        String[] allAliases = new String[]{
                ALIAS_CALCULATOR, ALIAS_NOTES, ALIAS_CLOCK, ALIAS_GAME,
                ALIAS_CAMERA, ALIAS_WEATHER, ALIAS_MUSIC, ALIAS_CUSTOM
        };

        for (String alias : allAliases) {
            try {
                pm.setComponentEnabledSetting(
                        new ComponentName(context, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
            } catch (Throwable ignored) {}
        }

        // 3. Remove Dynamic Shortcuts
        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context);
        } catch (Throwable ignored) {}

        // 4. Clear preferences
        try {
            getPrefs(context).edit()
                    .putBoolean(KEY_DISGUISE_ENABLED, false)
                    .putString(KEY_DISGUISE_TYPE, TYPE_NONE)
                    .putString(KEY_ACTIVE_ALIAS, "")
                    .apply();
        } catch (Throwable ignored) {}
    }

    /**
     * Launches the configured disguised target (added app or game).
     * Used when the user normally taps the app or when face match fails.
     */
    public boolean launchDisguisedTarget(Context context) {
        String pkg = getDisguisedPackageName(context);
        if (pkg != null && !pkg.trim().isEmpty()) {
            try {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
