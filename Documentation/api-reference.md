# Audience Manager API reference

## Prerequisites

Refer to the [Getting started guide](./getting-started.md)

## API reference

- [extensionVersion](#extensionversion)
- [getVisitorProfile](#getvisitorprofile)
- [registerExtension](#registerextension)
- [reset](#reset)
- [signalWithData](#signalwithdata)

------

## extensionVersion

The `extensionVersion()` API returns the version of the Audience extension that is registered with the Mobile Core extension.

To get the version of the Audience extension, use the following code sample:

#### Java

##### Syntax
```java
static @NonNull String extensionVersion();
```

##### Example
```java
String audienceExtensionVersion = Audience.extensionVersion();
```

#### Kotlin
##### Syntax
```kotlin
fun extensionVersion(): String
```

##### Example
```kotlin
val audienceExtensionVersion: String = Audience.extensionVersion()
```

## getVisitorProfile

This API returns the most recently obtained visitor profile. For easy access across multiple launches of your app, the visitor profile is saved in `SharedPreferences`. If no signal has been submitted, null is returned.

When an `AdobeCallbackWithError` is provided, an `AdobeError` can be returned in the eventuality of an unexpected error or if the default timeout (5000ms) is met before the callback is returned with the visitor profile.

#### Java

##### Syntax
```java
public static void getVisitorProfile(@NonNull final AdobeCallback<Map<String, String>> adobeCallback);
```

##### Example
```java
Audience.getVisitorProfile(new AdobeCallback<Map<String, String>>() {
    @Override
    public void call(final Map<String, String> visitorProfile) {
        // provide code to process the visitorProfile
    }
});
```

#### Kotlin

##### Syntax
```kotlin
fun getVisitorProfile(adobeCallback: AdobeCallback<Map<String, String?>?>)
```

##### Example
```kotlin
Audience.getVisitorProfile { visitorProfile ->
    // provide code to process the visitorProfile
}

```

## registerExtension

> **Warning**
> Deprecated as of 2.0.0. Use the [MobileCore.registerExtensions API](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md) instead.

Registers the Audience extension with `MobileCore`.

#### Java

##### Syntax
```java
public static void registerExtension();
```

##### Example
```java
Audience.registerExtension();
```

#### Kotlin

##### Syntax
```kotlin
fun registerExtension()
```

##### Example
```kotlin
Audience.registerExtension()
```

## reset

This API helps you reset the Audience Manager UUID and purges the current visitor profile.

#### Java

##### Syntax
```java
public static void reset()
```

##### Example
```java
Audience.reset();
```

#### Kotlin

##### Syntax
```kotlin
fun reset()
```

##### Example
```kotlin
Audience.reset()
```

## signalWithData

This method is used to send a signal with traits to Audience Manager and get the matching segments returned in a block callback. Audience Manager sends the UUID in response to an initial signal call. The UUID is persisted on local SDK storage and is sent by the SDK to Audience Manager in all subsequent signal requests.

If available, the ECID and other custom identifiers for the same visitor are sent with each signal request. The visitor profile that is returned by Audience Manager is saved in SDK local storage and is updated with subsequent signal calls.

For more information about the UUID and other Audience Manager identifiers, see the [index of IDs in Audience Manager](https://experienceleague.adobe.com/docs/audience-manager/user-guide/reference/ids-in-aam.html).

#### Java

##### Syntax
```java
public static void signalWithData(
		@NonNull final Map<String, String> data,
		@Nullable final AdobeCallback<Map<String, String>> adobeCallback
	)
```
* `data` is the traits data for the current visitor.
* `callback` is the void method that is invoked with the visitor's profile as a parameter.

##### Example
```java
final Map<String, String> traits = new HashMap<String, String>() {{
    put("trait1", "value1");
    put("trait2", "value2");
}};

Audience.signalWithData(traits, new AdobeCallback<Map<String, String>>() {
    @Override
    public void call(final Map<String, String> visitorProfile) {
        // handle the returned visitorProfile here
    }
});
```

#### Kotlin

##### Syntax
```kotlin
fun signalWithData(
        data: Map<String, String?>,
        adobeCallback: AdobeCallback<Map<String, String?>?>?)
```

##### Example
```kotlin
val traits: Map<String, String?> = mapOf(
    "trait1" to "value1",
    "trait2" to "value2"
)

Audience.signalWithData(traits) { visitorProfile ->
    // handle the returned visitorProfile
}
```
