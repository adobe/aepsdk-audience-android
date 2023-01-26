# Adobe Audience Manager Android Extension

## About this project

Adobe Audience Manager is a versatile audience data management platform. With the SDK, you can update audience profiles for users and retrieve user segment information from your mobile app. For more information, see [Adobe Audience Manager](https://business.adobe.com/products/audience-manager/adobe-audience-manager.html).

The Audience Manager mobile extension is an extension for the Android 2.x [Adobe Experience Platform SDK](https://developer.adobe.com/client-sdks/documentation/current-sdk-versions/) and requires the `Core` and `Identity` extensions for event handling.

To learn more, read the [Mobile Core](https://developer.adobe.com/client-sdks/documentation/mobile-core/) documentation.

### Installation

Integrate the Audience extension into your app by including the following in your gradle file's `dependencies`:

```gradle
implementation 'com.adobe.marketing.mobile:audience:2.+'
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:identity:2.+'
```

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio.

**Data Collection mobile property prerequisites**

The test app needs to be configured with the following extensions before it can be used:

- Mobile Core (installed by default)
- [Identity](https://developer.adobe.com/client-sdks/documentation/mobile-core/identity/)
- [Assurance](https://developer.adobe.com/client-sdks/documentation/platform-assurance-sdk/)

**Run test application**

1. In the test app, set your `ENVIRONMENT_FILE_ID` in `AudienceTestApp.java`.
2. Select the `app` runnable with the desired emulator and run the program.

**Inspect the events with Assurance**

Configure a new Assurance session by setting the Base URL to `audiencetestapp://main` and launch Assurance in the test app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "audiencetestapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.audiencetestapp
```

> Note: replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

## Related Projects

| Project                                                                              | Description                                                  |
| ------------------------------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core extensions](https://github.com/adobe/aepsdk-core-android)                      | The Core extension represents the foundation of the Adobe Experience Platform SDK. |
| [Assurance extension](https://github.com/adobe/aepsdk-assurance-android)             | The Assurance extension enables validation workflows for your SDK implementation. |
| [AEP SDK sample app for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK.                 |

## Documentation

Additional documentation for setup and usage can be found under the [Documentation](Documentation) directory.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
