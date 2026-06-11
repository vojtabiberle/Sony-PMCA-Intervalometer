package com.github.ma1co.pmcademo.app;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setStatus("ENTER/S2: take picture\nDELETE: exit");
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = CameraEx.open(0, null);
        ready = false;
        capturing = false;
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
            setStatus("Ready\nENTER/S2: take picture\nDELETE: exit");
        } catch (IOException e) {}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected boolean onFocusKeyDown() {
        camera.getNormalCamera().autoFocus(null);
        return true;
    }

    @Override
    protected boolean onFocusKeyUp() {
        camera.getNormalCamera().cancelAutoFocus();
        return true;
    }

    @Override
    protected boolean onShutterKeyDown() {
        takePicture();
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
        if (camera != null) {
            camera.cancelTakePicture();
        }
        return true;
    }

    @Override
    protected boolean onEnterKeyDown() {
        takePicture();
        return true;
    }

    private void takePicture() {
        if (!ready || capturing || camera == null) {
            return;
        }
        capturing = true;
        setStatus("Capturing...");
        camera.getNormalCamera().takePicture(null, null, null);
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
