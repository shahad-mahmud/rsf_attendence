package com.shahad.rsfattendence;
/*
 * Created by SHAHAD MAHMUD on 7/16/20
 */

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = new Intent(MainActivity.this, ActivityLogin.class);
        startActivity(intent);
        finish();

    }
}