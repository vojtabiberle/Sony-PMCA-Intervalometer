package com.github.ma1co.pmcademo.app;

import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import com.sony.scalar.hardware.CameraEx;

import java.io.IOException;

public class CameraActivity extends BaseActivity implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private CameraEx camera;
    private boolean ready;
    private boolean capturing;
    private int shotCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setStatus("HX90V Intervalometer 0.2\nwaiting for preview...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = CameraEx.open(0, null);
        ready = false;
        capturing = false;
        shotCount = 0;
        surfaceHolder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.release();
            camera = null;
        }
        ready = false;
        capturing = false;
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.getNormalCamera().setPreviewDisplay(holder);
            camera.getNormalCamera().startPreview();
            ready = true;
            setStatus("Ready\nENTER: test capture\nS2: disabled test\nMENU/DELETE: exit");
        } catch (IOException e) {}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected boolean onFocusKeyDown() {
        if (ready && camera != null) {
            setStatus("Focusing...");
            camera.getNormalCamera().autoFocus(null);
        }
        return true;
    }

    @Override
    protected boolean onFocusKeyUp() {
        if (camera != null) {
            camera.getNormalCamera().cancelAutoFocus();
            setStatus("Ready\nENTER: test capture\nS2: disabled test\nMENU/DELETE: exit");
        }
        return true;
    }

    @Override
    protected boolean onShutterKeyDown() {
        setStatus("S2 disabled in 0.2\nUse ENTER for capture test");
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
        setStatus("Ready\nENTER: test capture\nS2: disabled test\nMENU/DELETE: exit");
        return true;
    }

    @Override
    protected boolean onEnterKeyDown() {
        takePicture();
        return true;
    }

    @Override
    protected boolean onMenuKeyUp() {
        finish();
        return true;
    }

    @Override
    protected boolean onDeleteKeyUp() {
        finish();
        return true;
    }

    private void takePicture() {
        if (!ready || capturing || camera == null) {
            setStatus("Not ready\nready=" + ready + " capturing=" + capturing);
            return;
        }
        capturing = true;
        shotCount++;
        setStatus("Capturing test shot " + shotCount + "...");
        camera.getNormalCamera().takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera normalCamera) {
                capturing = false;
                try {
                    normalCamera.startPreview();
                    setStatus("Picture callback OK\nbytes=" + (data == null ? 0 : data.length) + "\nENTER: next test");
                } catch (RuntimeException e) {
                    setStatus("Callback OK, preview restart failed\n" + e.getClass().getSimpleName());
                }
            }
        });
    }

    private void setStatus(String status) {
        TextView textView = (TextView) findViewById(R.id.textView);
        if (textView != null) {
            textView.setText(status);
        }
    }

    @Override
    protected void setColorDepth(boolean highQuality) {
        super.setColorDepth(false);
    }
}
