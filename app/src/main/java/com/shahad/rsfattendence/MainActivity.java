package com.shahad.rsfattendence;
/*
 * Created by SHAHAD MAHMUD on 7/16/20
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PREF_FILE_NAME = "sharedPrefForLogIn";
    private static final String SHARED_PREF_KEY_IS_LOGGED_IN = "loginStatus";
    private static final String SHARED_PREF_KEY_LOGIN_TIME = "loginTime";

    private static final int AUTO_LOGIN_DURATION = 1800000; // automatic login if last login time was
    // less than 30 minutes.

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        long lastLoginTime = sharedPreferences.getLong(SHARED_PREF_KEY_LOGIN_TIME, 0);

        Log.d("MAIN_ACT", String.valueOf(System.currentTimeMillis() - lastLoginTime));

        if (System.currentTimeMillis() - lastLoginTime < AUTO_LOGIN_DURATION &&
                sharedPreferences.getBoolean(SHARED_PREF_KEY_IS_LOGGED_IN, false)) {
            Intent intent = new Intent(MainActivity.this, ActivitySendPresence.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this, ActivityLogin.class);
            startActivity(intent);
            finish();
        }

    }
}