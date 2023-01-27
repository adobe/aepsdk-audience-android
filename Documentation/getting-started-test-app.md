# Getting started with the test app

## Data Collection mobile property prerequisites

The test app needs to be configured with the following extensions before it can be used:

* Mobile Core (installed by default)
* [Identity](https://github.com/adobe/aepsdk-core-android)
* [Analytics](https://github.com/adobe/aepsdk-analytics-android)
* [Assurance](https://github.com/adobe/aepsdk-assurance-android)

## Run test application

1. In the test app, set your `ENVIRONMENT_FILE_ID` in `AudienceTestApp.java`.
2. Select the `app` runnable with the desired emulator and run the program.

## Validation with Assurance

Configure a new Assurance session by setting the Base URL to `audiencetestapp://main` and launch Assurance in the test app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "audiencetestapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.audiencetestapp
```

> **Note**
> Replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.
