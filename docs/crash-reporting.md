# Crash Reporting

Waterfox for Android uses a few libraries for crash and exception reporting. This kind of reporting gives Mozilla invaluable insight as to why Waterfox for Android crashes or incorrectly behaves. It is one of the key methods we use to improve the product in terms of stability.

This page documents the types of crash reporting, how the various parts interact, and what kind of data is sent back to Waterfox Project.

Documentation for the specific libraries is included in the [Android Components Crash Reporting README](https://github.com/mozilla-mobile/android-components/blob/main/components/lib/crash/README.md).

## Breadcrumbs

[Breadcrumbs](https://github.com/mozilla-mobile/android-components/blob/main/components/support/base/src/main/java/mozilla/components/support/base/crash/Breadcrumb.kt) are trail of events that are sent with each crash report to Sentry.  

### Events

In [HomeActivity](https://github.com/WaterfoxCo/Waterfox-Android/blob/main/app/src/main/java/org/mozilla/fenix/HomeActivity.kt) when `onDestinationChanged` occurs, the destination fragment's name and and whether it is a custom tab is added to the breadcrumbs.

## Sentry

[Sentry](https://sentry.io) is an open source crash reporting and aggregation platform. Both the client SDK, [github.com/getsentry/sentry-java](https://github.com/getsentry/sentry-java), and the server, [github.com/getsentry/sentry](https://github.com/getsentry/sentry), are open source.

A crash report is only sent when the user confirms and submits it through the crash reporter notification or dialog. Crash reports are never automatically sent.

### High-Level Summary

The server is hosted and maintained by Waterfox Project. There are no third-parties involved, all crash reports are sent directly from Waterfox for Android to the Sentry server hosted by Waterfox Project.

On the client side Sentry is invisible. There are no parts to interact with. It reports crashes and fatal errors back to Waterfox Project in the background.

On the server side there is a dashboard that the Waterfox for Android team uses to look at incoming crash reports. The dashboard lets us inspect the crash report in detail and for example see where in the application the crash happened, what version of the application was used and what version of Android OS was active. Below is an overview of all the attributes that are part of a crash report.

### Sentry Reports

A typical Sentry crash report contains three categories of data: device, application, crash. It also contains some metadata about the crash report:
```
 "id": "6ae18611d6c649529a5eda0e48f42cb4",
// ...
 "datetime": "2018-03-30T23:55:03.000000Z",
// ...
 "received": 1522454183.0,
```

To clarify, `id` is a unique identifier for this crash report, *not a unique identifier for the user sending the report.* We explicitly disable the ability to uniquely identify users from their crash reports.

#### Device Information

Sentry collects basic information about the device the application is running on. Both static (device type) and dynamic (memory in use, device orientation).

```
"contexts": {
    "device": {
        "screen_resolution":"1920x1080",
        "battery_level":44.0,
        "orientation":"portrait",
        "family":"ONEPLUS",
        "model_id":"PQ3A.190705.003",
        "type":"device",
        "low_memory":false,
        "simulator":false,
        "free_storage":21314179072,
        "storage_size":56416321536,
        "screen_dpi":420,
        "free_memory":2506031104,
        "memory_size":6005846016,
        "online":true,
        "charging":false,
        "model":"ONEPLUS A5000",
        "screen_density":2.625,
        "arch":"arm64-v8a",
        "brand":"OnePlus",
        "manufacturer":"OnePlus"
    },
// ...
    "os":{
        "rooted":true,
        "kernel_version":"4.4.184-sigmaKernel-v11.0",
        "version":"9",
        "build":"PQ3A.190705.003",
        "type":"os",
        "name":"Android"
    }
}
```

### Application Information

Sentry collects basic information about the Waterfox for Android app.

```
    "app":{
        "app_identifier":"net.waterfox.android",
        "app_name":"Waterfox Preview",
        "app_start_time":"2019-09-23T21:00:05Z",
        "app_version":"1.4.1",
        "type":"app",
        "app_build":12531634
    },
    "sdk":{
        "version":"1.7.10-598d4",
        "name":"sentry-java"
    }
```

### Crash Information

#### Stack trace

Every crash report contains a *stack trace*, which shows what functions in the Waterfox for Android code led to this crash. It includes names of Android framework functions and Waterfox for Android functions. Here's an excerpt of three lines from the stack trace:

```
  "sentry.interfaces.Exception": {
    "exc_omitted": null,
    "values": [
      {
        "stacktrace": {
          "frames": [
            {
              "function": "main",
              "abs_path": "ZygoteInit.java",
              "module": "com.android.internal.os.ZygoteInit",
              "in_app": false,
              "lineno": 801,
              "filename": "ZygoteInit.java"
            },
            {
              "function": "run",
              "abs_path": "ZygoteInit.java",
              "module": "com.android.internal.os.ZygoteInit$MethodAndArgsCaller",
              "in_app": false,
              "lineno": 911,
              "filename": "ZygoteInit.java"
            },
            {
              "function": "invoke",
              "abs_path": "Method.java",
              "in_app": false,
              "module": "java.lang.reflect.Method",
              "filename": "Method.java"
},
```

##### Exception message

The first line of every stack trace in every crash report contains a *reason* - why did this crash happen. This reason is provided by the developers who wrote the code that decide the app is in an error state. These developers include the Waterfox for Android team at Waterfox Project, the Android framework, the Java programming language, and any libraries Mozilla bundles to develop Waterfox for Android.

Java, the Android framework, Mozilla and Waterfox Project are diligent about making sure that no personally identifiable information is put in any of these messages. We keep them technical and to the point. We at Waterfox Project stay on top of our dependencies to ensure they're not including personally identifiable information as well.

Here's an example message generated by Java:
```
java.lang.StringIndexOutOfBoundsException: length=0; regionStart=20; regionLength=20
```

Example of a Waterfox for Android generated message:
```
java.lang.StringIndexOutOfBoundsException: Cannot create negative-length String
```

##### Raw data dump

In the explanations above, some redundant fields and field considered less important were omitted for brevity. To review these omissions, [this is an example of the raw data the server receives](https://gist.github.com/mcomella/50622aef817b40a20714b8550fb19991). This is up-to-date as of March 30, 2018.
