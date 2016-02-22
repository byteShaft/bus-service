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
    private static final String VIRGIN_KEY = "virgin";
    private static final String USER_NAME = "user_name";
    private static final String STUDENT_NAME = "student_name";
    private static final String ROUTE_STATUS = "route_status";
    private static final String STUDENT_TYPE = "student_type";

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean isVirgin() {
        return sPreferences.getBoolean(VIRGIN_KEY, true);
    }

    public static void setVirgin(boolean virgin) {
        sPreferences.edit().putBoolean(VIRGIN_KEY, virgin).apply();
    }

    public static String getUsername() {
        return sPreferences.getString(USER_NAME, null);
    }

    public static void putUsername(String username) {
        sPreferences.edit().putString(USER_NAME, username).apply();
    }

    public static String getStudentName() {
        return sPreferences.getString(STUDENT_NAME, null);
    }

    public static void putStudentName(String studentName) {
        sPreferences.edit().putString(STUDENT_NAME, studentName).apply();
    }
    public static boolean getRouteStatus() {
        return sPreferences.getBoolean(ROUTE_STATUS, true);
    }

    public static int getUserType() {
        return sPreferences.getInt(STUDENT_TYPE, 0);
    }

    public static void putUserType(int userType) {
        sPreferences.edit().putInt(STUDENT_TYPE, userType).apply();
    }

    public static void putRouteStatus(boolean status) {
        sPreferences.edit().putBoolean(ROUTE_STATUS, status).apply();
    }

    public static void replaceFragment(android.support.v4.app.FragmentManager fragMan, android.support.v4.app.Fragment frag) {
        FragmentTransaction transaction = fragMan.beginTransaction();
        transaction.replace(R.id.container_main, frag);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
