package com.nexachat.app.models;

import android.graphics.drawable.Drawable;
import java.io.Serializable;

public class InstalledAppInfo implements Serializable {
    private String appName;
    private String packageName;
    private boolean isSecured;
    private transient Drawable iconDrawable;

    public InstalledAppInfo() {
    }

    public InstalledAppInfo(String appName, String packageName, boolean isSecured) {
        this.appName = appName;
        this.packageName = packageName;
        this.isSecured = isSecured;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setSecured(boolean secured) {
        isSecured = secured;
    }

    public Drawable getIconDrawable() {
        return iconDrawable;
    }

    public void setIconDrawable(Drawable iconDrawable) {
        this.iconDrawable = iconDrawable;
    }
}
