package com.taibah.busservice.utils;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;

import com.taibah.busservice.R;

public class AppGlobals extends Application {

    private static Context sContext;
    private static SharedPreferences sPreferences;
    private static final String FIRST_RUN = "first_run";
    private static final String USER_NAME = "user_name";
    private static final String NAME = "student_name";
    private static final String ROUTE_STATUS = "route_status";
    private static final String STUDENT_TYPE = "student_type";
    private static final String TOKEN = "token";

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean isFirstRun() {
        return sPreferences.getBoolean(FIRST_RUN, true);
    }

    public static void setFirstRun(boolean firstRun) {
        sPreferences.edit().putBoolean(FIRST_RUN, firstRun).apply();
    }

    public static String getUsername() {
        return sPreferences.getString(USER_NAME, null);
    }

    public static void putUsername(String username) {
        sPreferences.edit().putString(USER_NAME, username).apply();
    }

    public static void putToken(String token) {
        sPreferences.edit().putString(TOKEN, token).apply();
    }

    public static void getToken(String token) {
        sPreferences.getString(TOKEN, token);
    }

    public static String getName() {
        return sPreferences.getString(NAME, null);
    }

    public static void putName(String name) {
        sPreferences.edit().putString(NAME, name).apply();
    }
    public static int getRouteStatus() {
        return sPreferences.getInt(ROUTE_STATUS, 0);
    }

    public static int getUserType() {
        return sPreferences.getInt(STUDENT_TYPE, 0);
    }

    public static void putUserType(int userType) {
        sPreferences.edit().putInt(STUDENT_TYPE, userType).apply();
    }

    public static void putRouteStatus(int status) {
        sPreferences.edit().putInt(ROUTE_STATUS, status).apply();
    }

    public static void replaceFragment(android.support.v4.app.FragmentManager fragMan, android.support.v4.app.Fragment frag) {
        FragmentTransaction transaction = fragMan.beginTransaction();
        transaction.replace(R.id.container_main, frag);
        transaction.commit();
    }
}
