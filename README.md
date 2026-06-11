# HX90V Intervalometer

Standalone intervalometer app for Sony DSC-HX90V / PlayMemories Camera Apps runtime.

The app is based on PMCADemo, but the camera path has been changed for HX90V:

- Uses `CameraEx.startSelfTimerShutter()` for capture.
- Resets the `CameraEx` session after every shot.
- Keeps auto power off disabled while the app is active.
- Hides the Android activity title bar.

## Current Stable Version

Version: `0.12`

Known-good commit:

```text
0110002 fix: default interval to zero seconds
```

## Controls

```text
S2                 manual shot, only when interval sequence is stopped
ENTER              start / stop interval sequence
UP / DOWN          interval between completed shots, default 0s
LEFT / RIGHT       number of shots, 0 means infinite
FN                 first-shot delay: 0s / 2s / 5s / 10s
MENU               exit app
DELETE             intentionally ignored
```

## Timing Model

`Delay` applies only before the first shot in an interval sequence.

`Interval` is the wait after a shot is completed and the camera session has been reset.

```text
ENTER
-> first delay
-> shot 1
-> reset camera session
-> interval
-> shot 2
-> reset camera session
-> interval
-> shot 3
```

With `Interval 0s`, the next shot starts as soon as the previous shot has completed and the camera has been reopened.

## Stable Capture Path

The stock Android `android.hardware.Camera.takePicture(...)` path causes the HX90V to restart.

The stable path is:

1. Open camera with `CameraEx.open(0, null)`.
2. Start preview.
3. Trigger shot with `CameraEx.startSelfTimerShutter()`.
4. Wait for capture callback.
5. Release the camera.
6. Reopen `CameraEx`.
7. Restart preview.

Do not remove the per-shot camera reset unless a replacement has been tested on the real camera.

## Test Checklist

Before treating a build as stable:

1. Start the app and confirm preview appears.
2. Take three manual shots with `S2`.
3. Run a sequence with `Delay 2s`, `Interval 0s`, `Shots 30`.
4. Run a longer astro-like sequence with the intended exposure settings.
5. Stop a running sequence with `ENTER`.
6. Exit with `MENU`.

## Build

From this directory:

```sh
GRADLE_USER_HOME=$PWD/../android-build/gradle-home \
JAVA_HOME=$PWD/../android-build/jdk8 \
../android-build/gradle-4.10.3/bin/gradle assembleDebug --no-daemon --stacktrace
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

From the workspace root:

```sh
tools/pmca-venv/bin/python tools/Sony-PMCA-RE/pmca-console.py install \
  -f tools/HX90VIntervalometerApp/app/build/outputs/apk/debug/app-debug.apk
```
