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
    private boolean resetting;
    private boolean shutterDown;
    private boolean enterDown;
    private int shotCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setStatus("HX90V Intervalometer 0.8\nwaiting for preview...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = CameraEx.open(0, null);
        ready = false;
        capturing = false;
        resetting = false;
        shutterDown = false;
        enterDown = false;
        shotCount = 0;
        surfaceHolder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        ready = false;
        capturing = false;
        resetting = false;
        shutterDown = false;
        enterDown = false;
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
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
            if (!capturing) {
                setReadyStatus();
            }
        }
        return true;
    }

    @Override
    protected boolean onShutterKeyDown() {
        if (shutterDown) {
            return true;
        }
        shutterDown = true;
        takePicture();
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
        shutterDown = false;
        return true;
    }

    @Override
    protected boolean onEnterKeyDown() {
        if (enterDown) {
            return true;
        }
        enterDown = true;
        takePicture();
        return true;
    }

    @Override
    protected boolean onEnterKeyUp() {
        enterDown = false;
        return true;
    }

    @Override
    protected boolean onMenuKeyDown() {
        finish();
        return true;
    }

    @Override
    protected boolean onMenuKeyUp() {
        finish();
        return true;
    }

    @Override
    protected boolean onDeleteKeyDown() {
        return true;
    }

    @Override
    protected boolean onDeleteKeyUp() {
        return true;
    }

    private void takePicture() {
        if (!ready || capturing || resetting || camera == null) {
            setStatus("Not ready\nready=" + ready + " capturing=" + capturing + "\nresetting=" + resetting);
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
                resetting = false;
                setStatus("CameraEx error " + error + "\nS2/ENTER: retry");
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
                resetCameraSession();
            }
        });
        camera.setStoreImageCompleteListener(new CameraEx.StoreImageCompleteListener() {
            @Override
            public void onDone(int status, CameraEx.StoreImageInfo info, CameraEx camera) {
                capturing = false;
                resetCameraSession();
            }
        });
    }

    private void resetCameraSession() {
        if (resetting) {
            return;
        }
        resetting = true;
        ready = false;
        setStatus("Resetting camera...");
        releaseCamera();
        try {
            camera = CameraEx.open(0, null);
            startPreview(surfaceHolder);
        } catch (RuntimeException e) {
            setStatus("Camera reopen failed\n" + e.getClass().getSimpleName() + "\nMENU: exit");
        }
    }

    private void startPreview(SurfaceHolder holder) {
        if (camera == null || holder == null) {
            return;
        }
        try {
            camera.getNormalCamera().setPreviewDisplay(holder);
            camera.getNormalCamera().startPreview();
            ready = true;
            capturing = false;
            resetting = false;
            shutterDown = false;
            enterDown = false;
            registerCameraExListeners();
            setReadyStatus();
        } catch (IOException e) {
            resetting = false;
            setStatus("Preview failed\n" + e.getClass().getSimpleName() + "\nMENU: exit");
        } catch (RuntimeException e) {
            resetting = false;
            setStatus("Preview failed\n" + e.getClass().getSimpleName() + "\nMENU: exit");
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.release();
            } catch (RuntimeException e) {
            }
            camera = null;
        }
    }

    private void setReadyStatus() {
        setStatus("Ready\nS2/ENTER: take picture\nMENU: exit");
    }

    private void setStatus(String status) {
        final String finalStatus = status;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.textView);
                if (textView != null) {
                    textView.setText(finalStatus);
                }
            }
        });
    }

    @Override
    protected void setColorDepth(boolean highQuality) {
        super.setColorDepth(false);
    }
}
