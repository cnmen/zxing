package com.cmonbaby.zxing.demo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cmonbaby.zxing.demo.utils.BeepManager;
import com.cmonbaby.zxing.demo.utils.Utils;
import com.cmonbaby.zxing.library.CaptureCallback;
import com.cmonbaby.zxing.library.camera.CameraManager;
import com.cmonbaby.zxing.library.decode.DecodeThread;
import com.cmonbaby.zxing.library.utils.CaptureActivityHandler;
import com.cmonbaby.zxing.library.utils.InactivityTimer;
import com.google.zxing.Result;

import java.io.IOException;

/**
 * <p>Author:      Simon
 * <p>QO:          8950764
 * <p>Email:       simon@cmonbaby.com
 * <p>WebSize:     https://www.cmonbaby.com
 * <p>Version:     1.1.0
 * <p>Date:        2019/11/26
 * <p>Description: Custom Capture Activity
 */
public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, CaptureCallback {

    private Context context;
    private TextView tvLight;
    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;

    private boolean isPause = false;
    private CaptureActivityHandler handler;
    private Rect mCropRect = null;
    private CameraManager cameraManager;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private ObjectAnimator objectAnimator;
    private boolean isHasSurface = false;

    private final static String TAG = "simon >>> ";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        // KEEP SCREEN ON
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        context = this;

        init();
        initScan();
    }

    private void init() {
        tvLight = findViewById(R.id.tv_light);
        ToggleButton tbLight = findViewById(R.id.tb_light);

        tbLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvLight.setText("关灯");
                    Utils.openFlashlight(cameraManager);
                } else {
                    tvLight.setText("开灯");
                    Utils.closeFlashlight();
                }
            }
        });

        findViewById(R.id.ll_album).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.openAlbum(context);
            }
        });
    }

    private void initScan() {
        scanPreview = findViewById(R.id.capture_preview);
        scanContainer = findViewById(R.id.capture_container);
        scanCropView = findViewById(R.id.capture_crop_view);
        ImageView scanLine = findViewById(R.id.scan_line);

        float curTranslationY = scanLine.getTranslationY();
        objectAnimator = ObjectAnimator.ofFloat(scanLine, "translationY",
                curTranslationY, Utils.dp2px(this, 170));
        objectAnimator.setDuration(4000);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        pauseScan();
        super.onPause();
    }

    private void startScan() {
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        if (isPause) {
            objectAnimator.resume();
            isPause = false;
        } else {
            objectAnimator.start();
        }

        cameraManager = new CameraManager(getApplication());
        handler = null;
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            scanPreview.getHolder().addCallback(this);
        }
        inactivityTimer.onResume();
    }

    private void pauseScan() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        objectAnimator.pause();
        isPause = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "SurfaceHolder为空");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void handleDecode(Result result, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        Intent intent = new Intent();
        intent.putExtra(Utils.BAR_CODE, result.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("surfaceHolder为空");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "摄像头已打开");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }

            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            Utils.displayFrameworkBugMessageAndExit(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected error initializing camera", e);
            Utils.displayFrameworkBugMessageAndExit(context);
        }
    }

    private void initCrop() {
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - Utils.getStatusBarHeight(context);

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        int x = cropLeft * (cameraManager.getCameraResolution().y) / containerWidth;
        int y = cropTop * (cameraManager.getCameraResolution().x) / containerHeight;

        int width = cropWidth * (cameraManager.getCameraResolution().y) / containerWidth;
        int height = cropHeight * (cameraManager.getCameraResolution().x) / containerHeight;

        mCropRect = new Rect(x, y, width + x, height + y);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Utils.SELECT_PIC && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = Utils.getPath(context, uri);
            Result result = Utils.scanningImage(path);
            if (result == null) {
                Toast.makeText(context, "未发现二维码/条形码", Toast.LENGTH_LONG).show();
            } else {
                String recode = Utils.recode(result.toString());
                Toast.makeText(context, recode, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == Utils.SELECT_PIC_KITKAT && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = Utils.getPath(context, uri);
            Result result = Utils.scanningImage(path);
            if (result == null) {
                Toast.makeText(context, "未发现二维码/条形码", Toast.LENGTH_LONG).show();
            } else {
                String recode = Utils.recode(result.toString());
                Toast.makeText(context, recode, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        if (objectAnimator != null) {
            objectAnimator.end();
        }
        super.onDestroy();
    }
}
