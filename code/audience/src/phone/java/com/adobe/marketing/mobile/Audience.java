/* ***********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2018 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

package com.adobe.marketing.mobile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adobe.marketing.mobile.audience.AudienceExtension;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Audience {
	private static final String EXTENSION_VERSION = "2.0.0";
	private static final String LOG_TAG = "Audience";
	private static final String CLASS_NAME = "Audience";

	// config defaults
	private static final int CALLBACK_TIMEOUT_MILLIS = 5000;

	public static final @NonNull Class<? extends Extension> EXTENSION = AudienceExtension.class;

	private Audience() { }

	/**
	 * Returns the current version of the Audience extension.
	 *
	 * @return A {@link String} representing the Audience extension version
	 */
	public static @NonNull String extensionVersion() {
		return EXTENSION_VERSION;
	}

	/**
	 * Registers the Audience extension with the {@code MobileCore}.
	 * <p>
	 * This will allow the extension to send and receive events to and from the SDK.
	 *
	 * @deprecated as of 2.0.0, use {@link com.adobe.marketing.mobile.MobileCore#registerExtensions(List, AdobeCallback)} with {@link Audience#EXTENSION} instead.
	 */
	@Deprecated
	public static void registerExtension() {
		MobileCore.registerExtension(AudienceExtension.class, extensionError -> {
			if (extensionError != null) {
				Log.error(LOG_TAG, CLASS_NAME, "There was an error registering the Audience Extension: %s", extensionError.getErrorName());
			}
		});
	}

	/**
	 * Returns the visitor profile that was most recently obtained.
	 * <p>
	 * Visitor profile is saved in {@link android.content.SharedPreferences} for easy access across multiple launches of your app.
	 * If no audience signal has been submitted yet, null is returned.
	 *
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the visitor's profile as a parameter;
	 *        when an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the eventuality of an unexpected error.
	 *
	 * @see #signalWithData(Map, AdobeCallback)
	 */
	public static void getVisitorProfile(@NonNull final AdobeCallback<Map<String, String>> adobeCallback) {
		identityRequest(EventDataKeys.AAM.VISITOR_PROFILE, adobeCallback);
	}

	/**
	 * Resets the Audience Manager UUID and purges the current visitor profile from {@link android.content.SharedPreferences}.
	 * <p>
	 * Audience reset also clears the current in-memory DPID and DPUUID variables.
	 */
	public static void reset() {
		final Event event = new Event.Builder("AudienceRequestReset", EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET).build();
		MobileCore.dispatchEvent(event);
		Log.debug(LOG_TAG, CLASS_NAME, "Request to reset Audience Manager values for this device has been dispatched.");
	}

	/**
	 * Sends Audience Manager a signal with traits and gets the matching segments for the visitor.
	 * <p>
	 * Audience manager sends UUID in response to initial signal call. The UUID is persisted in
	 * {@link android.content.SharedPreferences} and sent by SDK in all subsequent signal requests. If you are using
	 * Experience Cloud ID Service, then Experience Cloud ID (MID) and other customerIDs for the same visitor are also sent
	 * in each signal request along with DPID and DPUUID. The visitor profile that Audience Manager returns is saved in
	 * {@code SharedPreferences} and updated with every signal call.
	 *
	 * @param data          traits data for the current visitor
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the visitor's profile as a parameter;
	 *        when an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *        eventuality of an unexpected error or if the default timeout (5000ms) is met before the callback is returned with AAM profile.
	 */
	public static void signalWithData(@NonNull final Map<String, String> data,
									  @Nullable final AdobeCallback<Map<String, String>> adobeCallback) {
		final Map<String, Object> eventData = new HashMap<String, Object>() {{ put(EventDataKeys.AAM.VISITOR_TRAITS, data); }};
		final Event event = new Event.Builder("AudienceRequestContent", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
				.setEventData(eventData).build();

		Log.debug(LOG_TAG, CLASS_NAME,"Audience Profile data was submitted: %s", data.toString());
		MobileCore.dispatchEventWithResponseCallback(event, CALLBACK_TIMEOUT_MILLIS, new AdobeCallbackWithError<Event>() {
			@Override
			public void fail(AdobeError adobeError) {
				Log.warning(LOG_TAG, CLASS_NAME, "An error occurred dispatching Audience Profile data: %s", adobeError.getErrorName());
				if (adobeCallback != null) {
					adobeCallback.call(null);
				}
			}

			@Override
			public void call(Event event) {
				if (adobeCallback == null) {
					return;
				}

				final Map<String, Object> eventData = event.getEventData();
				final Map<String, String> profileMap = (Map<String, String>) eventData.get(EventDataKeys.AAM.VISITOR_PROFILE);
				adobeCallback.call(profileMap);
			}
		});
	}

	/**
	 * Initiates an Audience Manager Identity Request event.
	 * Currently used to get dpid, dpuuid, and user profiles from aam module
	 *
	 * @param keyName  		(required) key in which this method will search the resulting event's
	 *                      eventdata for to provide in the callback
	 * @param callback 		(required) {@link AdobeCallback} method which will be called with the appropriate value depending on the keyName param
	 *
	 * @see EventDataKeys.AAM
	 */
	private static void identityRequest(@NonNull final String keyName, @NonNull final AdobeCallback<Map<String, String>> callback) {
		// both parameters are required
		if (StringUtils.isNullOrEmpty(keyName) || callback == null) {
			Log.debug(LOG_TAG, CLASS_NAME, "Failed to send Identity request due to missing parameters in the call; keyName is empty or Callback is null");
			return;
		}

		final Event event = new Event.Builder("AudienceRequestIdentity", EventType.AUDIENCEMANAGER, EventSource.REQUEST_IDENTITY)
				.build();

		Log.debug(LOG_TAG, CLASS_NAME, "Dispatching Identity request event: %s", event);
		MobileCore.dispatchEventWithResponseCallback(event, CALLBACK_TIMEOUT_MILLIS, new AdobeCallbackWithError<Event>() {
			@Override
			public void fail(AdobeError adobeError) {
				Log.warning(LOG_TAG, CLASS_NAME, "An error occurred dispatching Audience Profile data: %s", adobeError.getErrorName());
				if (callback != null) {
					callback.call(null);
				}
			}

			@Override
			public void call(Event event) {
				final Map<String, Object> eventData = event.getEventData();

				if (keyName.equals(EventDataKeys.AAM.AUDIENCE_IDS)) {
					final String dpid = (String) eventData.get(EventDataKeys.AAM.DPID);
					final String dpuuid = (String) eventData.get(EventDataKeys.AAM.DPUUID);
					final Map<String, String> value = new HashMap<String, String>() {{
						put(EventDataKeys.AAM.DPID, dpid);
						put(EventDataKeys.AAM.DPUUID, dpuuid);
					}};
					callback.call(value);

				} else if (keyName.equals(EventDataKeys.AAM.VISITOR_PROFILE)) {
					final Map<String, String> value = (Map<String, String>) eventData.get(keyName);
					callback.call(value != null ? value : new HashMap<String, String>());
				} else {
					Log.debug(LOG_TAG, CLASS_NAME, "Attempting to process the response from an identityRequest but the requested value (%s) was not found.", keyName);
					callback.call(null);
				}
			}
		});
	}

	private static final class EventDataKeys {
		private EventDataKeys() {}

		static final class AAM {
			// request keys
			static final String VISITOR_TRAITS = "aamtraits";

			// response keys
			static final String AUDIENCE_IDS = "audienceids";
			static final String DPID = "dpid";
			static final String DPUUID = "dpuuid";
			static final String VISITOR_PROFILE = "aamprofile";

			private AAM() {}
		}
	}
}

