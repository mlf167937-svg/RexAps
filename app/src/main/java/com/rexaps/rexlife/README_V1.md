# RexLife V1 integration notes

## What is included

- RexLife is registered as a first-class RexAps module in `ui/RexAppRegistry.kt`.
- `ui/RexRoute.kt` defines the `REXLIFE` route.
- `ui/RexApsApp/RexApsAppDialogs.kt` opens `com.rexaps.rexlife.RexLifeScreen`.
- Module artwork includes a RexLife heart/pulse mark.
- The RexLife dashboard uses `MaterialTheme.colorScheme`, so it inherits the active RexAps theme.
- `MySavings` supports manual income/expense transactions, transaction edit/delete, savings goals, and progress.
- `MySchool` supports weekly lesson schedules, uniforms by weekday, homework from scheduled subjects or manual entry, deadlines, optional page/question references, completion state, and edit/delete.
- MyHealth, MyTasks, MyNotes, and MyPlanner are explicit “Segera Hadir” placeholders.
- There is no notification, alarm, or notification-sound code in V1.
- Data is persisted as JSON at `data/rexlife.json` beneath the RexLife storage root.

## Requested public folder

Preferred path:

`/storage/emulated/0/Download/RexAps/RexLife/data/rexlife.json`

Android 11/API 30 and newer restrict direct access to shared storage. For this direct-file implementation, merge the following permission into the real app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

For Android 9/API 28 through Android 10/API 29, add the legacy permission and request it at runtime:

```xml
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />
```

If the app targets Android 10/API 29 and uses direct filesystem access, verify its scoped-storage configuration for the project's target SDK and test on a real device. `MANAGE_EXTERNAL_STORAGE` is subject to platform and app-distribution policy; do not request it unless the app's distribution method and purpose permit it.

Until the app has usable public-folder access, RexLife visibly reports that it is using app-private storage at `files/RexLife/data/rexlife.json`, so changes are still saved instead of silently failing. After granting access, tap **Coba lagi** to migrate the current in-memory data to the requested folder.

## Archive/build limitation

The supplied archive contains source Kotlin files only. It does not contain the app manifest, Gradle settings, build scripts, or Gradle wrapper. Therefore the Android build and device runtime could not be verified from this archive. Add the files to the matching Android source sets, merge the manifest permissions where appropriate, and build/test with the full project before installing.
