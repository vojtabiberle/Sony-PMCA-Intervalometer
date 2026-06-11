# Sony PMCA Intervalometer

Standalone intervalometer app for Sony cameras that can run PlayMemories Camera Apps / PMCA-style custom apps.

The app is based on PMCADemo, with the camera path changed to use the Sony camera extension API:

- Uses `CameraEx.startSelfTimerShutter()` for capture.
- Resets the `CameraEx` session after every shot.
- Keeps auto power off disabled while the app is active.
- Hides the Android activity title bar.

## Current Version

Version: `0.13`

Package id: `cz.bazil.sony.pmca.intervalometer`

If you previously installed the HX90V-specific test build, uninstall it before installing this renamed app.

Stable tag: `v0.13-stable`

## Tested Cameras

Confirmed working:

- Sony DSC-HX90V, firmware version 1

Tested HX90V behavior:

- `S2` manual shot works.
- `ENTER` interval sequence start/stop works.
- `MENU` exits the app without restarting the camera.
- `10` shots with `Interval 0s` works.
- `30` shots with `Interval 0s` works.
- `30` shots with `Interval 5s` works.

## Likely Compatibility

This app may also work on other Sony cameras from the PlayMemories Camera Apps generation, especially models where custom PMCA apps can be installed and the Sony `CameraEx` API behaves similarly.

It is not expected to work on newer Sony cameras that cannot run PMCA/custom camera apps.

Untested areas on other models:

- whether `CameraEx.startSelfTimerShutter()` works
- whether the camera needs the same per-shot camera reset
- key mappings for `S2`, `ENTER`, `FN`, arrows, and `MENU`
- whether `MENU` exits cleanly
- whether long sequences remain stable

## Test Reports Wanted

If you test another camera model, please report:

- exact camera model
- firmware version
- whether the app installs and starts
- whether preview appears
- whether `S2` takes one shot
- whether `ENTER` starts and stops a sequence
- whether `MENU` exits without rebooting the camera
- longest sequence tested, including interval and exposure settings
- any key mapping differences

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

On the HX90V, the stock Android `android.hardware.Camera.takePicture(...)` path causes the camera to restart.

The stable path is:

1. Open camera with `CameraEx.open(0, null)`.
2. Start preview.
3. Trigger shot with `CameraEx.startSelfTimerShutter()`.
4. Wait for capture callback.
5. Release the camera.
6. Reopen `CameraEx`.
7. Restart preview.

Do not remove the per-shot camera reset unless a replacement has been tested on a real camera.

## Test Checklist

Before treating a build as stable on a model:

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

Release APK:

```text
releases/sony-pmca-intervalometer-0.13-debug.apk
```

## Install

From the workspace root:

```sh
tools/pmca-venv/bin/python tools/Sony-PMCA-RE/pmca-console.py install \
  -f tools/HX90VIntervalometerApp/app/build/outputs/apk/debug/app-debug.apk
```

Or install the release APK:

```sh
tools/pmca-venv/bin/python tools/Sony-PMCA-RE/pmca-console.py install \
  -f tools/HX90VIntervalometerApp/releases/sony-pmca-intervalometer-0.13-debug.apk
```
