/*
  Copyright 2018 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.audience.AudienceExtension;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Audience {

	private static final String LOG_TAG = "Audience";
	private static final String LOG_SOURCE = "Audience";

	private static final String EXTENSION_VERSION = "2.0.0";
	private static final int CALLBACK_TIMEOUT_MILLIS = 5000;

	@NonNull
	public static final Class<? extends Extension> EXTENSION = AudienceExtension.class;

	private Audience() {}

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
	 * @deprecated as of 2.0.0, use {@link com.adobe.marketing.mobile.MobileCore#registerExtensions(List, AdobeCallback)} with Audience.EXTENSION instead.
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public static void registerExtension() {
		MobileCore.registerExtension(
			AudienceExtension.class,
			extensionError -> {
				if (extensionError != null) {
					Log.error(
						LOG_TAG,
						LOG_SOURCE,
						"There was an error registering the Audience extension: %s",
						extensionError.getErrorName()
					);
				}
			}
		);
	}

	/**
	 * Returns the visitor profile that was most recently obtained.
	 * <p>
	 * Visitor profile is saved persistently for easy access across multiple launches of your app.
	 * If no audience signal has been submitted yet, null is returned.
	 *
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the visitor's profile as a parameter;
	 *        when an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the eventuality of an unexpected error.
	 *
	 * @see #signalWithData(Map, AdobeCallback)
	 */
	public static void getVisitorProfile(@NonNull final AdobeCallback<Map<String, String>> adobeCallback) {
		if (adobeCallback == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Unexpected null callback, provide a callback to retrieve current visitorProfile."
			);
			return;
		}

		final Event event = new Event.Builder(
			"AudienceRequestIdentity",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_IDENTITY
		)
			.build();

		Log.debug(LOG_TAG, LOG_SOURCE, "Dispatching Audience IdentityRequest event: %s", event);
		MobileCore.dispatchEventWithResponseCallback(
			event,
			CALLBACK_TIMEOUT_MILLIS,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(final AdobeError adobeError) {
					Log.warning(
						LOG_TAG,
						LOG_SOURCE,
						"An error occurred retrieving Audience Profile data: %s",
						adobeError.getErrorName()
					);
					final AdobeCallbackWithError<?> adobeCallbackWithError = adobeCallback instanceof AdobeCallbackWithError
						? (AdobeCallbackWithError<?>) adobeCallback
						: null;
					if (adobeCallbackWithError != null) {
						adobeCallbackWithError.fail(adobeError);
					}
				}

				@Override
				public void call(final Event event) {
					final Map<String, String> value = DataReader.optStringMap(
						event.getEventData(),
						EventDataKeys.VISITOR_PROFILE,
						null
					);
					adobeCallback.call(value);
				}
			}
		);
	}

	/**
	 * Sends Audience Manager a signal with traits and gets the matching segments for the visitor.
	 * <p>
	 * Audience manager sends UUID in response to initial signal call. The UUID is persisted
	 * and sent by SDK in all subsequent signal requests. If you are using
	 * Experience Cloud ID Service, then Experience Cloud ID (MID) and other customerIDs for the same visitor are also sent
	 * in each signal request.
	 *
	 * @param data          traits data for the current visitor
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the visitor's profile as a parameter;
	 *        when an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *        eventuality of an unexpected error or if the default timeout (5000ms) is met before the callback is returned with AAM profile.
	 */
	public static void signalWithData(
		@NonNull final Map<String, String> data,
		@Nullable final AdobeCallback<Map<String, String>> adobeCallback
	) {
		final Map<String, Object> eventData = new HashMap<String, Object>() {
			{
				put(EventDataKeys.VISITOR_TRAITS, data);
			}
		};
		final Event event = new Event.Builder(
			"AudienceRequestContent",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(eventData)
			.build();

		Log.debug(
			LOG_TAG,
			LOG_SOURCE,
			"Audience event was submitted for signalWithData: %s",
			data != null ? data.toString() : "no data"
		);
		if (adobeCallback == null) {
			MobileCore.dispatchEvent(event);
			return;
		}

		MobileCore.dispatchEventWithResponseCallback(
			event,
			CALLBACK_TIMEOUT_MILLIS,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(final AdobeError adobeError) {
					Log.warning(
						LOG_TAG,
						LOG_SOURCE,
						"An error occurred dispatching Audience Profile data: %s",
						adobeError.getErrorName()
					);

					final AdobeCallbackWithError<?> adobeCallbackWithError = adobeCallback instanceof AdobeCallbackWithError
						? (AdobeCallbackWithError<?>) adobeCallback
						: null;
					if (adobeCallbackWithError != null) {
						adobeCallbackWithError.fail(adobeError);
					}
				}

				@Override
				public void call(final Event event) {
					final Map<String, String> profileMap = DataReader.optStringMap(
						event.getEventData(),
						EventDataKeys.VISITOR_PROFILE,
						null
					);
					adobeCallback.call(profileMap);
				}
			}
		);
	}

	/**
	 * Resets the Audience Manager UUID and purges the current visitor profile from persistence.
	 */
	public static void reset() {
		final Event event = new Event.Builder(
			"AudienceRequestReset",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_RESET
		)
			.build();
		MobileCore.dispatchEvent(event);
		Log.debug(LOG_TAG, LOG_SOURCE, "Request to reset Audience Manager values for this device has been dispatched.");
	}

	private static final class EventDataKeys {

		private EventDataKeys() {}

		// request keys
		static final String VISITOR_TRAITS = "aamtraits";

		// response keys
		static final String VISITOR_PROFILE = "aamprofile";
	}
}
