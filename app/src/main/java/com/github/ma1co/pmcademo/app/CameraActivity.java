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
    private int shotCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setStatus("HX90V Intervalometer 0.4\nwaiting for preview...");
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
            registerCameraExListeners();
            setStatus("Ready\nS2/ENTER: take picture\nMENU/DELETE: exit");
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
            setStatus("Ready\nS2/ENTER: take picture\nMENU/DELETE: exit");
        }
        return true;
    }

    @Override
    protected boolean onShutterKeyDown() {
        takePicture();
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
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
        setStatus("Taking picture " + shotCount + "...");
        camera.startSelfTimerShutter();
    }

    private void registerCameraExListeners() {
        camera.setErrorCallback(new CameraEx.ErrorCallback() {
            @Override
            public void onError(int error, CameraEx camera) {
                capturing = false;
                setStatus("CameraEx error " + error + "\nENTER: retry");
            }
        });
        camera.setShutterListener(new CameraEx.ShutterListener() {
            @Override
            public void onShutter(int status, CameraEx camera) {
                setStatus("Shutter event status=" + status + "\nwaiting for store...");
            }
        });
        camera.setCaptureStatusListener(new CameraEx.OnCaptureStatusListener() {
            @Override
            public void onStart(int status, CameraEx camera) {
                setStatus("Capture started status=" + status);
            }

            @Override
            public void onEnd(int status, int reason, CameraEx camera) {
                capturing = false;
                setStatus("Capture ended status=" + status + " reason=" + reason + "\nENTER: next test");
            }
        });
        camera.setStoreImageCompleteListener(new CameraEx.StoreImageCompleteListener() {
            @Override
            public void onDone(int status, CameraEx.StoreImageInfo info, CameraEx camera) {
                capturing = false;
                setStatus("Store complete status=" + status + "\nENTER: next test");
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
