package com.demo.suspendbuttonlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.laocaixw.layout.SuspendButtonLayout;

public class Main2Activity extends AppCompatActivity {

    private SuspendButtonLayout suspendButtonLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        suspendButtonLayout = (SuspendButtonLayout) findViewById(R.id.layout);
        suspendButtonLayout.setOnSuspendListener(new SuspendButtonLayout.OnSuspendListener() {
            @Override
            public void onButtonStatusChanged(int status) {
                switch (status) {
                    case SuspendButtonLayout.SUSPEND_BUTTON_MOVING:
                        // 移动时改变图片
                        suspendButtonLayout.setMainCloseImageResource(R.mipmap.ic_launcher);
                        break;
                    case SuspendButtonLayout.SUSPEND_BUTTON_MOVED:
                        // 移动到位后改变图片
                        suspendButtonLayout.setMainCloseImageResource(R.mipmap.suspend_main_close);
                        break;
                }
            }

            @Override
            public void onChildButtonClick(int index) {
                Toast.makeText(Main2Activity.this, "" + index, Toast.LENGTH_SHORT).show();
            }
        });
        suspendButtonLayout.setPosition(true, 100);
        findViewById(R.id.hide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.hideSuspendButton();
            }
        });
        findViewById(R.id.show).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.showSuspendButton();
            }
        });
        findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.openSuspendButton();
            }
        });
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.closeSuspendButton();
            }
        });
        findViewById(R.id.setimage_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.setChildImageResource(1, R.mipmap.ic_launcher);
            }
        });
        findViewById(R.id.resetimage_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.setChildImageResource(1, R.mipmap.suspend_1);
            }
        });
        findViewById(R.id.setmainopenimage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.setMainOpenImageResource(R.mipmap.ic_launcher);
            }
        });
        findViewById(R.id.resetmainopenimage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.setMainOpenImageResource(R.mipmap.suspend_main_open);
            }
        });
        findViewById(R.id.changepos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suspendButtonLayout.setPosition(Math.random() * 100 > 50,
                        (int) (Math.random() * 100));
            }
        });
    }

}
