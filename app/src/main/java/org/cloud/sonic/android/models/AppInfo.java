package org.cloud.sonic.android.models;

/**
 * @author Eason
 * App Message
 */
public class AppInfo {

    public String appName = "";

    public String packageName = "";

    public String versionName = "";

    public int versionCode = 0;

    public String appIcon = null;

    @Override
    public String toString() {
        return "AppInfo{" +
            "appName='" + appName + '\'' +
            ", packageName='" + packageName + '\'' +
            ", versionName='" + versionName + '\'' +
            ", versionCode=" + versionCode +
            ", appIcon=" + appIcon +
            '}';
    }
}
