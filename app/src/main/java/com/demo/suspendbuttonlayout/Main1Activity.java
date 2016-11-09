package com.demo.suspendbuttonlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class Main1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

}
