package com.github.ma1co.pmcademo.app;

import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.TextView;
import com.sony.scalar.hardware.CameraEx;

import java.io.IOException;

public class CameraActivity extends BaseActivity implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private CameraEx camera;
    private Handler handler;
    private boolean ready;
    private boolean capturing;
    private boolean resetting;
    private boolean intervalRunning;
    private boolean currentShotFromInterval;
    private boolean waitingForFirstShot;
    private boolean shutterDown;
    private boolean enterDown;
    private int shotCount;
    private int intervalShotCount;
    private int intervalSeconds = 0;
    private int firstDelaySeconds = 2;
    private int targetShots;

    private final Runnable firstIntervalShot = new Runnable() {
        @Override
        public void run() {
            waitingForFirstShot = false;
            if (intervalRunning && ready && !capturing && !resetting) {
                takePicture(true);
            }
        }
    };

    private final Runnable nextIntervalShot = new Runnable() {
        @Override
        public void run() {
            if (intervalRunning && ready && !capturing && !resetting) {
                takePicture(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        handler = new Handler();
        setStatus("HX90V Intervalometer 0.10\nwaiting for preview...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = CameraEx.open(0, null);
        ready = false;
        capturing = false;
        resetting = false;
        intervalRunning = false;
        currentShotFromInterval = false;
        waitingForFirstShot = false;
        shutterDown = false;
        enterDown = false;
        shotCount = 0;
        intervalShotCount = 0;
        surfaceHolder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        ready = false;
        capturing = false;
        resetting = false;
        intervalRunning = false;
        currentShotFromInterval = false;
        waitingForFirstShot = false;
        shutterDown = false;
        enterDown = false;
        handler.removeCallbacks(firstIntervalShot);
        handler.removeCallbacks(nextIntervalShot);
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
        if (!intervalRunning) {
            takePicture(false);
        } else {
            setStatus(formatStatus("Running", "S2 ignored"));
        }
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
        toggleInterval();
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

    @Override
    protected boolean onUpKeyDown() {
        if (!intervalRunning) {
            intervalSeconds = Math.min(600, intervalSeconds + intervalStep());
            setReadyStatus();
        }
        return true;
    }

    @Override
    protected boolean onDownKeyDown() {
        if (!intervalRunning) {
            intervalSeconds = Math.max(0, intervalSeconds - intervalStep());
            setReadyStatus();
        }
        return true;
    }

    @Override
    protected boolean onRightKeyDown() {
        if (!intervalRunning) {
            targetShots = Math.min(999, targetShots + 10);
            setReadyStatus();
        }
        return true;
    }

    @Override
    protected boolean onLeftKeyDown() {
        if (!intervalRunning) {
            targetShots = Math.max(0, targetShots - 10);
            setReadyStatus();
        }
        return true;
    }

    @Override
    protected boolean onFnKeyDown() {
        if (!intervalRunning) {
            if (firstDelaySeconds == 0) {
                firstDelaySeconds = 2;
            } else if (firstDelaySeconds == 2) {
                firstDelaySeconds = 5;
            } else if (firstDelaySeconds == 5) {
                firstDelaySeconds = 10;
            } else {
                firstDelaySeconds = 0;
            }
            setReadyStatus();
        }
        return true;
    }

    private int intervalStep() {
        return intervalSeconds < 10 ? 1 : 5;
    }

    private void toggleInterval() {
        if (intervalRunning) {
            stopInterval("Stopped");
            return;
        }
        if (!ready || capturing || resetting || camera == null) {
            setStatus(formatStatus("Not ready", null));
            return;
        }
        intervalRunning = true;
        waitingForFirstShot = firstDelaySeconds > 0;
        intervalShotCount = 0;
        if (waitingForFirstShot) {
            setStatus(formatStatus("Starting in " + firstDelaySeconds + "s", formatSequenceCount()));
            handler.removeCallbacks(firstIntervalShot);
            handler.postDelayed(firstIntervalShot, firstDelaySeconds * 1000L);
        } else {
            takePicture(true);
        }
    }

    private void stopInterval(String state) {
        intervalRunning = false;
        currentShotFromInterval = false;
        waitingForFirstShot = false;
        handler.removeCallbacks(firstIntervalShot);
        handler.removeCallbacks(nextIntervalShot);
        setStatus(formatStatus(state, null));
    }

    private void takePicture(boolean fromInterval) {
        if (!ready || capturing || resetting || waitingForFirstShot || camera == null) {
            setStatus(formatStatus("Not ready", "ready=" + ready + " capturing=" + capturing + " resetting=" + resetting));
            return;
        }
        capturing = true;
        currentShotFromInterval = fromInterval;
        if (fromInterval) {
            intervalShotCount++;
        }
        shotCount++;
        setStatus(formatStatus("Taking picture " + shotCount, fromInterval ? "Sequence " + formatSequenceCount() : "Manual"));
        camera.startSelfTimerShutter();
    }

    private void registerCameraExListeners() {
        camera.setErrorCallback(new CameraEx.ErrorCallback() {
            @Override
            public void onError(int error, CameraEx camera) {
                capturing = false;
                resetting = false;
                stopInterval("CameraEx error " + error);
            }
        });
        camera.setShutterListener(new CameraEx.ShutterListener() {
            @Override
            public void onShutter(int status, CameraEx camera) {
                setStatus(formatStatus("Shutter status=" + status, null));
            }
        });
        camera.setCaptureStatusListener(new CameraEx.OnCaptureStatusListener() {
            @Override
            public void onStart(int status, CameraEx camera) {
                setStatus(formatStatus("Capture started", "status=" + status));
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
        setStatus(formatStatus("Resetting camera", null));
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
            onCameraReadyAfterReset();
        } catch (IOException e) {
            resetting = false;
            stopInterval("Preview failed " + e.getClass().getSimpleName());
        } catch (RuntimeException e) {
            resetting = false;
            stopInterval("Preview failed " + e.getClass().getSimpleName());
        }
    }

    private void onCameraReadyAfterReset() {
        if (intervalRunning && currentShotFromInterval) {
            currentShotFromInterval = false;
            if (targetShots > 0 && intervalShotCount >= targetShots) {
                stopInterval("Done");
            } else {
                setStatus(formatStatus("Waiting " + intervalSeconds + "s", formatSequenceCount()));
                handler.removeCallbacks(nextIntervalShot);
                handler.postDelayed(nextIntervalShot, intervalSeconds * 1000L);
            }
        } else {
            currentShotFromInterval = false;
            setReadyStatus();
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
        setStatus(formatStatus("Ready", null));
    }

    private String formatSequenceCount() {
        return intervalShotCount + "/" + (targetShots == 0 ? "inf" : Integer.toString(targetShots));
    }

    private String formatStatus(String state, String detail) {
        String shots = targetShots == 0 ? "inf" : Integer.toString(targetShots);
        String status = state + "\nInterval " + intervalSeconds + "s  Shots " + shots + "  Delay " + firstDelaySeconds + "s\nS2 manual  ENTER run/stop\nUP/DOWN interval  LEFT/RIGHT shots\nFN first delay  MENU exit";
        if (detail != null) {
            status += "\n" + detail;
        }
        return status;
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
