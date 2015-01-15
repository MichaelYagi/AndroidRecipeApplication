package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Cached variables
/******************************************************************/

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.Arrays;

/**
 * Created by Michael on 12/28/2014.
 */
public class SaveSharedPreference {
    private static final String[] admins = {"myagi"};
    static final String PREF_USERNAME= "username";
    static final String PREF_PASSWORD = "password";
    static final String PREF_EMAIL = "email";
    static final String PREF_ID = "0";


    static SharedPreferences getSharedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static boolean isAdmin(Context ctx) {
        if (getUsername(ctx).length() > 0 && Arrays.asList(admins).contains(getUsername(ctx))) {
            return true;
        } else {
            return false;
        }
    }

    public static void setUsername(Context ctx, String username) {
        Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(PREF_USERNAME, username);
        editor.commit();
    }

    public static String getUsername(Context ctx) {
        return getSharedPreferences(ctx).getString(PREF_USERNAME, "");
    }

    public static void setPassword(Context ctx, String password) {
        Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(PREF_PASSWORD, password);
        editor.commit();
    }

    public static String getPassword(Context ctx) {
        return getSharedPreferences(ctx).getString(PREF_PASSWORD, "");
    }

    public static void setEmail(Context ctx, String email) {
        Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(PREF_EMAIL, email);
        editor.commit();
    }

    public static String getEmail(Context ctx) {
        return getSharedPreferences(ctx).getString(PREF_EMAIL, "");
    }

    public static void setUserID(Context ctx, int userID) {
        Editor editor = getSharedPreferences(ctx).edit();
        String id = Integer.toString(userID);
        editor.putString(PREF_ID, id);
        editor.commit();
    }

    public static int getUserID(Context ctx) {
        int id = Integer.parseInt(getSharedPreferences(ctx).getString(PREF_ID, "0"));
        return id;
    }

    public static void clearCredentials(Context ctx) {
        Editor editor = getSharedPreferences(ctx).edit();
        editor.clear(); //clear all stored data
        editor.commit();
    }

}

