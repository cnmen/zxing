package com.cmonbaby.zxing.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.cmonbaby.zxing.demo.utils.Utils;

/**
 * <p>Author:      Simon
 * <p>QO:          8950764
 * <p>Email:       simon@cmonbaby.com
 * <p>WebSize:     https://www.cmonbaby.com
 * <p>Version:     1.1.0
 * <p>Date:        2019/11/26
 * <p>Description: Sample coding
 */
public class MainActivity extends Activity {

    private static final int REQUEST_SCAN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        findViewById(R.id.ll_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRuntimePermission();
            }
        });
    }

    // Sample coding for permission
    private void getRuntimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 200);
            } else {
                jumpScanPage();
            }
        }
    }

    private void jumpScanPage() {
        startActivityForResult(new Intent(this, CaptureActivity.class), REQUEST_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN && resultCode == RESULT_OK) {
            Toast.makeText(this, data.getStringExtra(Utils.BAR_CODE), Toast.LENGTH_LONG).show();
        }
    }
}
