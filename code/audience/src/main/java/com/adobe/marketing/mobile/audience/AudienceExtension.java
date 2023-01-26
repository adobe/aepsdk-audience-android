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

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.audience.AudienceConstants.FRIENDLY_EXTENSION_NAME;
import static com.adobe.marketing.mobile.audience.AudienceConstants.LOG_TAG;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.PersistentHitQueue;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.URLBuilder;
import com.adobe.marketing.mobile.util.UrlUtils;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The AudienceExtension enables interactions with Audience Manager.
 * <ol>
 *     <li>The primary interaction is via the customer submitting user traits to the Audience Manager server.</li>
 *     <li>The Audience Manager server is responsible for responding with segments to which the user belongs,
 *     based on the submitted user traits.</li>
 *     <li>The Audience extension has a database to store impending network requests, which enables profile updates to
 *     still be collected and reported once the user's device comes back online.</li>
 * </ol>
 *
 * The AudienceExtension listens for the following {@link Event}s:
 * <ol>
 *   <li>{@link EventType#ANALYTICS} - {@link EventSource#RESPONSE_CONTENT}</li>
 *   <li>{@link EventType#AUDIENCEMANAGER} - {@link EventSource#REQUEST_CONTENT}</li>
 *   <li>{@link EventType#AUDIENCEMANAGER} - {@link EventSource#REQUEST_IDENTITY}</li>
 *   <li>{@link EventType#AUDIENCEMANAGER} - {@link EventSource#REQUEST_RESET}</li>
 *   <li>{@link EventType#CONFIGURATION} - {@link EventSource#RESPONSE_CONTENT}</li>
 *   <li>{@link EventType#GENERIC_IDENTITY} - {@link EventSource#REQUEST_RESET}</li>
 *   <li>{@link EventType#LIFECYCLE} - {@link EventSource#RESPONSE_CONTENT}</li>
 *</ol>
 *
 * The AudienceExtension dispatches the following {@code Event}s:
 * <ol>
 *   <li>{@link EventType#AUDIENCEMANAGER} - {@link EventSource#RESPONSE_CONTENT}</li>
 *   <li>{@link EventType#AUDIENCEMANAGER} - {@link EventSource#RESPONSE_IDENTITY}</li>
 * </ol>
 */
public final class AudienceExtension extends Extension {

	private static final String LOG_SOURCE = "AudienceExtension";

	private final AudienceState internalState;
	private final PersistentHitQueue hitQueue;

	@VisibleForTesting
	final AudienceNetworkResponseHandler networkResponseHandler;

	private class NetworkResponseHandler implements AudienceNetworkResponseHandler {

		private AudienceState state;

		NetworkResponseHandler(final AudienceState state) {
			this.state = state;
		}

		@Override
		public void complete(final String responsePayload, final Event requestEvent) {
			final String LOG_SOURCE = "AudienceNetworkResponseHandler";
			if (requestEvent == null) {
				Log.warning(LOG_TAG, LOG_SOURCE, "Unable to process network response, invalid request event.");
				return;
			}

			if (requestEvent.getTimestamp() < state.getLastResetTimestampMillis()) {
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					"Not dispatching Audience hit response since resetIdentities API was called after queuing this hit."
				);
				return;
			}

			Map<String, String> profile = new HashMap<>();

			if (StringUtils.isNullOrEmpty(responsePayload)) {
				Log.warning(LOG_TAG, LOG_SOURCE, "Null/empty response from server, nothing to process.");
				shareStateForEvent(requestEvent);
				dispatchAudienceResponseContent(profile, requestEvent);
				return;
			}

			// process the response from the AAM server and share the shared state
			profile = processResponse(responsePayload, requestEvent);

			// if profile is empty, there was a json error in the response, don't dispatch a generic event
			if (profile != null && !profile.isEmpty()) {
				dispatchAudienceResponseContent(profile, null);
			}

			// dispatch paired event
			dispatchAudienceResponseContent(profile, requestEvent);
		}
	}

	AudienceExtension(final ExtensionApi extensionApi) {
		this(extensionApi, null);
	}

	@VisibleForTesting
	AudienceExtension(final ExtensionApi extensionApi, final AudienceState audienceState) {
		super(extensionApi);
		this.internalState = audienceState != null ? audienceState : new AudienceState();
		networkResponseHandler = new NetworkResponseHandler(internalState);
		final DataQueue dataQueue = ServiceProvider.getInstance().getDataQueueService().getDataQueue(getName());
		this.hitQueue = new PersistentHitQueue(dataQueue, new AudienceHitProcessor(networkResponseHandler));
	}

	//region Extension interface methods

	/**
	 * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
	 *
	 * @return A {@link String} extension name for Audience
	 */
	@NonNull
	@Override
	protected String getName() {
		return EXTENSION_NAME;
	}

	/**
	 * Overridden method of {@link Extension} class to provide a friendly extension name.
	 *
	 * @return A {@link String} friendly extension name for Audience
	 */
	@NonNull
	@Override
	protected String getFriendlyName() {
		return FRIENDLY_EXTENSION_NAME;
	}

	/**
	 * Overridden method of {@link Extension} class to provide the extension version.
	 *
	 * @return A {@link String} representing the extension version
	 */
	@NonNull
	@Override
	protected String getVersion() {
		return com.adobe.marketing.mobile.Audience.extensionVersion();
	}

	@Override
	protected void onRegistered() {
		getApi()
			.registerEventListener(EventType.ANALYTICS, EventSource.RESPONSE_CONTENT, this::handleAnalyticsResponse);
		getApi()
			.registerEventListener(
				EventType.AUDIENCEMANAGER,
				EventSource.REQUEST_CONTENT,
				this::handleAudienceRequestContent
			);
		getApi()
			.registerEventListener(
				EventType.AUDIENCEMANAGER,
				EventSource.REQUEST_IDENTITY,
				this::handleAudienceRequestIdentity
			);
		getApi()
			.registerEventListener(EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET, this::handleResetIdentities);
		getApi()
			.registerEventListener(
				EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT,
				this::handleConfigurationResponse
			);
		getApi()
			.registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET, this::handleResetIdentities);
		getApi()
			.registerEventListener(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT, this::handleLifecycleResponse);

		Log.trace(LOG_TAG, LOG_SOURCE, "Dispatching Audience shared state");
		shareStateForEvent(null);
	}

	@Override
	public boolean readyForEvent(@NonNull final Event event) {
		final SharedStateResult configSharedState = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
			event
		);

		// signal events require both config and identity shared states
		if (
			(
				event.getType().equals(EventType.AUDIENCEMANAGER) &&
				event.getSource().equals(EventSource.REQUEST_CONTENT)
			) ||
			(event.getType().equals(EventType.LIFECYCLE) && event.getSource().equals(EventSource.RESPONSE_CONTENT))
		) {
			final SharedStateResult identitySharedState = getSharedStateForExtension(
				AudienceConstants.EventDataKeys.Identity.MODULE_NAME,
				event
			);
			return (
				configSharedState.getStatus() != SharedStateStatus.PENDING &&
				identitySharedState.getStatus() != SharedStateStatus.PENDING
			);
		}

		return configSharedState.getStatus() == SharedStateStatus.SET;
	}

	//endregion

	//region Event listeners
	@VisibleForTesting
	void handleConfigurationResponse(@NonNull final Event event) {
		// check privacy status. if not found, .UNKNOWN privacy status will be used
		final Map<String, Object> eventData = event.getEventData();
		final MobilePrivacyStatus privacyStatus = MobilePrivacyStatus.fromString(
			DataReader.optString(eventData, AudienceConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "")
		);
		internalState.setMobilePrivacyStatus(privacyStatus);
		hitQueue.handlePrivacyChange(privacyStatus);

		if (privacyStatus.equals(MobilePrivacyStatus.OPT_OUT)) {
			sendOptOutHit(eventData);
		}
		shareStateForEvent(event);
	}

	/**
	 * Resets UUID, and the Audience Manager Visitor Profile from the {@code AudienceState} instance.
	 * <p>
	 * This method also updates shared state for version provided by {@code event} param.
	 *
	 * @param event {@link Event} containing instruction to reset identities
	 */
	@VisibleForTesting
	void handleResetIdentities(@NonNull final Event event) {
		Log.debug(LOG_TAG, LOG_SOURCE, "Resetting stored Audience Manager identities and visitor profile.");
		if (EventType.GENERIC_IDENTITY.equals(event.getType())) {
			hitQueue.clear();
		}

		internalState.clearIdentifiers();
		internalState.setLastResetTimestamp(event.getTimestamp());
		shareStateForEvent(event);
	}

	private void handleAnalyticsResponse(@NonNull final Event event) {
		if (!serverSideForwardingToAam(event)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Not processing Analytics response event - AAM forwarding is disabled in configuration."
			);
			return;
		}

		final String response = DataReader.optString(
			event.getEventData(),
			AudienceConstants.EventDataKeys.Analytics.ANALYTICS_SERVER_RESPONSE_KEY,
			""
		);
		if (StringUtils.isNullOrEmpty(response)) {
			Log.trace(LOG_TAG, LOG_SOURCE, "Ignoring Analytics response event - the response is null or empty.");
			return;
		}

		processResponse(response, event);
	}

	/**
	 * Handles the signalWithData API by attempting to send the Audience Manager hit containing the passed-in event data.
	 *
	 * If a response is received for the processed `AudienceHit`, a response content event with visitor profile data is dispatched.
	 *
	 * @param event The event coming from the signalWithData API invocation
	 */
	@VisibleForTesting
	void handleAudienceRequestContent(@NonNull final Event event) {
		submitSignal(event);
	}

	/**
	 * Handles the getVisitorProfile API by dispatching a response content event containing the visitor profile stored in the {@link AudienceState}.
	 *
	 * @param event the event coming from the getVisitorProfile API invocation
	 */
	@VisibleForTesting
	void handleAudienceRequestIdentity(@NonNull final Event event) {
		final Map<String, Object> responseEventData = new HashMap<>();
		responseEventData.put(
			AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE,
			internalState.getVisitorProfile()
		);

		final Event responseEvent = new Event.Builder(
			"Audience Manager Identities",
			EventType.AUDIENCEMANAGER,
			EventSource.RESPONSE_IDENTITY
		)
			.setEventData(responseEventData)
			.inResponseToEvent(event)
			.build();

		getApi().dispatch(responseEvent);
	}

	/**
	 * Processes Lifecycle Response content and sends a signal to Audience Manager if aam forwarding is disabled.
	 *
	 * The Audience Manager shared state will be updated on Lifecycle Start events.
	 *
	 * @param event: The lifecycle response event
	 */
	@VisibleForTesting
	void handleLifecycleResponse(@NonNull final Event event) {
		// if aam forwarding is enabled, we don't need to send anything
		if (serverSideForwardingToAam(event)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring Lifecycle response event because AAM forwarding is enabled in configuration."
			);
			return;
		}

		final Map<String, Object> eventData = event.getEventData();
		if (MapUtils.isNullOrEmpty(eventData)) {
			Log.trace(LOG_TAG, LOG_SOURCE, "Ignoring Lifecycle response event with absent or empty event data.");
			return;
		}

		// send a signal
		final Map<String, String> tempLifecycleData = DataReader.optStringMap(
			eventData,
			AudienceConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA,
			null
		);
		final Map<String, String> lifecycleContextData = getLifecycleDataForAudience(tempLifecycleData);

		final Map<String, Object> newEventData = new HashMap<String, Object>() {
			{
				put(AudienceConstants.EventDataKeys.Audience.VISITOR_TRAITS, lifecycleContextData);
			}
		};

		final Event newCurrentEvent = new Event.Builder(
			"Audience Manager Profile",
			EventType.AUDIENCEMANAGER,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(newEventData)
			.build();

		// TODO: set timestamp and event number???
		submitSignal(newCurrentEvent);
	}

	//endregion

	/**
	 * Processes a response from Audience Manager upon signal request.
	 * <p>
	 * Attempts to forward any necessary requests found in the AAM "dests" array, and creates a {@code Map<String, String>}
	 * out of the contents of the "stuff" array.
	 * <p>
	 * This method also helps persist {@code uuid} in response with {@link AudienceState} for use in subsequent signal calls.
	 *
	 * @param response {@link String} representation of the JSON response from the Audience Manager server
	 * @param event {@link Event} instance
	 * @return a {@code Map<String, String>} containing the user's AAM segments
	 */
	private Map<String, String> processResponse(final String response, final Event event) {
		if (StringUtils.isNullOrEmpty(response)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to process Audience Manager server response - response was null or empty."
			);
			return null;
		}

		// get timeout from config
		final SharedStateResult configSharedState = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
			event
		);
		if (configSharedState == null || configSharedState.getStatus() == SharedStateStatus.PENDING) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to process Audience Manager server response - configuration shared state is pending."
			);
			return null;
		}

		final int timeout = DataReader.optInt(
			configSharedState.getValue(),
			AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
			AudienceConstants.DEFAULT_AAM_TIMEOUT
		);

		JSONObject jsonResponse;
		try {
			jsonResponse = new JSONObject(response);
		} catch (JSONException ex) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Failed to parse response from Audience Manager server - %s",
				ex.getLocalizedMessage()
			);
			return null;
		}

		// check "dests" for urls to send
		processDestsArray(jsonResponse, timeout);

		try {
			// save uuid for use with subsequent calls
			// Setting the UUID may fail if the AudienceState's current privacy is opt-out
			internalState.setUuid(jsonResponse.getString(AudienceConstants.AUDIENCE_MANAGER_JSON_USER_ID_KEY));
		} catch (final JSONException ex) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to retrieve UUID from Audience Manager response - %s",
				ex.getLocalizedMessage()
			);
		}

		// process the "stuff" array
		final Map<String, String> returnedMap = processStuffArray(jsonResponse);

		if (returnedMap.size() > 0) {
			Log.trace(LOG_TAG, LOG_SOURCE, "Response received from Audience Manager server - %s", returnedMap);
		} else {
			Log.trace(LOG_TAG, LOG_SOURCE, "Response received from Audience Manager server was empty.");
		}

		// save profile in defaults
		// Note, the AudienceState may have a different privacy status than that of the calling event.
		// Setting the visitor profile may fail if the AudienceState's current privacy is opt-out
		internalState.setVisitorProfile(returnedMap);

		shareStateForEvent(event);

		return returnedMap;
	}

	private SharedStateResult getSharedStateForExtension(final String extensionName, final Event event) {
		return getApi().getSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
	}

	private void shareStateForEvent(final Event event) {
		getApi().createSharedState(internalState.getStateData(), event);
	}

	private boolean serverSideForwardingToAam(final Event event) {
		final SharedStateResult configSharedState = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
			event
		);
		if (configSharedState.getStatus() != SharedStateStatus.SET) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Attempted to retrieve AAM configuration for server-side forwarding but shared state was not set."
			);
			return false;
		}
		return DataReader.optBoolean(
			configSharedState.getValue(),
			AudienceConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
			false
		);
	}

	/**
	 * Sends a signal to the Audience Manager server.
	 * <p>
	 * A signal is not submitted to Audience Manager if,
	 * <ul>
	 *     <li>{@code Configuration} shared state is unavailable.</li>
	 *     <li>{@link com.adobe.marketing.mobile.audience.AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER} is not configured and is null or empty.</li>
	 *     <li>{@link com.adobe.marketing.mobile.audience.AudienceConstants.EventDataKeys.Configuration#GLOBAL_CONFIG_PRIVACY} is set to {@link MobilePrivacyStatus#OPT_OUT}.</li>
	 * </ul>
	 * Additionally, if privacy status is not set and is {@link MobilePrivacyStatus#UNKNOWN}, then signal hits are queued until user opts in or out.
	 *
	 * @param event {@link Event} containing data to be sent to the Audience Manager.
	 */
	private void submitSignal(final Event event) {
		// make sure we have configuration first
		final SharedStateResult configSharedState = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
			event
		);
		final Map<String, Object> configData = configSharedState.getValue();

		final String server = DataReader.optString(
			configData,
			AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER,
			null
		);
		final int timeout = DataReader.optInt(
			configData,
			AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
			AudienceConstants.DEFAULT_AAM_TIMEOUT
		);
		final MobilePrivacyStatus privacyStatus = MobilePrivacyStatus.fromString(
			DataReader.optString(
				configData,
				AudienceConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY,
				AudienceConstants.DEFAULT_PRIVACY_STATUS.getValue()
			)
		);

		// make sure we have configuration before we move on
		if (StringUtils.isNullOrEmpty(server) || privacyStatus == MobilePrivacyStatus.OPT_OUT) {
			// create an empty valid shared state if privacy is opt-out.
			// if not configured, dispatch an empty event for the pairId (if necessary)
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Dispatching an empty profile - AAM server configuration is unavailable or privacy status is opted-out."
			);
			dispatchAudienceResponseContent(null, event);
			return;
		}

		if (event.getTimestamp() < internalState.getLastResetTimestampMillis()) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Dropping Audience hit, resetIdentities API was called after this request.");
			dispatchAudienceResponseContent(null, event);
			return;
		}

		if (privacyStatus == MobilePrivacyStatus.UNKNOWN) {
			// when privacy is opt-unknown, callback should be immediately called with empty data.
			Log.debug(LOG_TAG, LOG_SOURCE, "Dispatching an empty profile - privacy status is unknown.");
			dispatchAudienceResponseContent(null, event);
		}

		if (hitQueue == null) {
			Log.warning(LOG_TAG, LOG_SOURCE, "Unable to queue AAM request as Audience Database not initialized.");
			return;
		}

		// generate the url to send
		final String requestUrl = buildSignalUrl(server, event);
		Log.debug(LOG_TAG, LOG_SOURCE, "Queuing request - %s", requestUrl);
		AudienceDataEntity entity = new AudienceDataEntity(event, requestUrl, timeout);
		hitQueue.queue(entity.toDataEntity());
	}

	/**
	 * Invokes the dispatcher passing the current visitor {@code profile} and {@code event} to dispatch {@code AUDIENCEMANAGER},
	 * {@code RESPONSE_CONTENT} event.
	 *
	 * @param profile {@code Map<String, String>} containing the user's profile
	 * @param event request {@link Event} object to be used for dispatching the paired response event;
	 *              if null is provided the response will be generic, not tied to a request id
	 */
	private void dispatchAudienceResponseContent(final Map<String, String> profile, final Event event) {
		final Map<String, Object> eventData = new HashMap<String, Object>() {
			{
				put(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE, profile);
			}
		};

		Event responseEvent;
		if (event != null) {
			// dispatch paired response if there is a request event
			responseEvent =
				new Event.Builder("Audience Manager Profile", EventType.AUDIENCEMANAGER, EventSource.RESPONSE_CONTENT)
					.setEventData(eventData)
					.inResponseToEvent(event)
					.build();
		} else {
			responseEvent =
				new Event.Builder("Audience Manager Profile", EventType.AUDIENCEMANAGER, EventSource.RESPONSE_CONTENT)
					.setEventData(eventData)
					.build();
		}

		getApi().dispatch(responseEvent);
	}

	/**
	 * Send an opt-out hit to the AAM server that has been configured.
	 *
	 * <p>
	 * If the {@link AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER} has been configured, and {@link AudienceState#getUuid()} returns
	 * an UUID value, the hit will be sent out.
	 * <br>
	 * The result of whether the hit was sent out or not is then dispatched as a {@link EventSource#RESPONSE_CONTENT} event.
	 *
	 * @param configuration The configuration {@code Map<String,Object>} object
	 */
	private void sendOptOutHit(final Map<String, Object> configuration) {
		// If opt-out, and we have a UUID, then send an opt-out hit, and dispatch an event
		final String aamServer = DataReader.optString(
			configuration,
			AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER,
			null
		);
		final String uuid = internalState.getUuid();

		boolean canSendOptOutHit = !StringUtils.isNullOrEmpty(aamServer) && !StringUtils.isNullOrEmpty(uuid);

		if (canSendOptOutHit) {
			// Send the opt-out hit
			String optOutUrl = getOptOutUrlPrefix(aamServer) + getOptOutUrlSuffix(uuid);

			final int timeout = DataReader.optInt(
				configuration,
				AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
				AudienceConstants.DEFAULT_AAM_TIMEOUT
			);

			// We don't expect a response back. This is a fire and forget
			final NetworkRequest networkRequest = new NetworkRequest(
				optOutUrl,
				HttpMethod.GET,
				null,
				null,
				timeout,
				timeout
			);
			ServiceProvider
				.getInstance()
				.getNetworkService()
				.connectAsync(
					networkRequest,
					connection -> {
						if (connection == null) {
							return;
						}

						if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
							Log.trace(LOG_TAG, LOG_SOURCE, "Successfully sent the optOut hit.");
						} else {
							Log.trace(
								LOG_TAG,
								LOG_SOURCE,
								"Failed to send the optOut hit with connection status (%s).",
								connection.getResponseCode()
							);
						}

						connection.close();
					}
				);
		}

		final Map<String, Object> eventData = new HashMap<String, Object>() {
			{
				put(AudienceConstants.EventDataKeys.Audience.OPTED_OUT_HIT_SENT, canSendOptOutHit);
			}
		};
		final Event optOutEvent = new Event.Builder(
			"Audience Manager Opt Out Event",
			EventType.AUDIENCEMANAGER,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(eventData)
			.build();
		getApi().dispatch(optOutEvent);
	}

	/**
	 * Generates the URL prefix used to send a opt-out hit.
	 *
	 * @param server The {@link AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER} value
	 *
	 * @return The formatted url prefix
	 * @see AudienceConstants#AUDIENCE_MANAGER_OPT_OUT_URL_BASE
	 */
	private String getOptOutUrlPrefix(final String server) {
		return String.format(AudienceConstants.AUDIENCE_MANAGER_OPT_OUT_URL_BASE, server);
	}

	/**
	 * Generates the URL suffix used to send a opt-out hit.
	 *
	 * @param uuid The {@code uuid} value that will get attached to the opt-out URL suffix
	 *
	 * @return The formatted url suffix
	 */
	private String getOptOutUrlSuffix(final String uuid) {
		return String.format(AudienceConstants.AUDIENCE_MANAGER_OPT_OUT_URL_AAM, uuid);
	}

	/**
	 * Converts data from a lifecycle event into its form desired by Audience Manager.
	 *
	 * @param lifecycleData map containing event data ({@code Map<String, Object>}) from a {@code Lifecycle} {@link Event}
	 * @return {@code HashMap<String, String>} containing Lifecycle data transformed for Audience Manager
	 */
	private HashMap<String, String> getLifecycleDataForAudience(final Map<String, String> lifecycleData) {
		final HashMap<String, String> lifecycleContextData = new HashMap<>();

		if (MapUtils.isNullOrEmpty(lifecycleData)) {
			return lifecycleContextData;
		}

		// copy the event's data so we don't accidentally overwrite it for someone else consuming this event
		final Map<String, String> tempLifecycleContextData = new HashMap<>(lifecycleData);

		for (Map.Entry<String, String> kvp : AudienceConstants.MAP_TO_CONTEXT_DATA_KEYS.entrySet()) {
			final String value = tempLifecycleContextData.get(kvp.getKey());

			if (!StringUtils.isNullOrEmpty(value)) {
				lifecycleContextData.put(kvp.getValue(), value);
				tempLifecycleContextData.remove(kvp.getKey());
			}
		}

		lifecycleContextData.putAll(tempLifecycleContextData);

		return lifecycleContextData;
	}

	/**
	 * Builds the URL used to send a signal to Audience Manager.
	 * <p>
	 * Customer provided KVPs are added as URL parameters to be used as traits for the signal.
	 *
	 * @param server {@link String} containing name of the server
	 * @param event {@link Event} instance
	 * @return {@code String} representation of the URL to be used
	 */
	private String buildSignalUrl(final String server, final Event event) {
		// get traits from event
		final Map<String, Object> customerEventData = event.getEventData();
		final Map<String, String> customerData = customerEventData == null
			? null
			: DataReader.optStringMap(customerEventData, AudienceConstants.EventDataKeys.Audience.VISITOR_TRAITS, null);

		final String urlString = new URLBuilder()
			.enableSSL(true)
			.setServer(server)
			.addPath(AudienceConstants.AUDIENCE_MANAGER_EVENT_PATH)
			.addQuery(getCustomUrlVariables(customerData), URLBuilder.EncodeType.NONE)
			.addQuery(getDataProviderUrlVariables(event), URLBuilder.EncodeType.NONE)
			.addQuery(getPlatformSuffix(), URLBuilder.EncodeType.NONE)
			.addQuery(AudienceConstants.AUDIENCE_MANAGER_URL_PARAM_DST, URLBuilder.EncodeType.NONE)
			.addQuery(AudienceConstants.AUDIENCE_MANAGER_URL_PARAM_RTBD, URLBuilder.EncodeType.NONE)
			.build();

		return urlString;
	}

	/**
	 * Processes the provided map of customer data and converts them for use as URL parameters.
	 *
	 * @param data {@code Map<String, String>} of customer data to be converted
	 * @return {@link String} representing value of URL parameters
	 */
	private String getCustomUrlVariables(final Map<String, String> data) {
		if (MapUtils.isNullOrEmpty(data)) {
			Log.debug(LOG_TAG, LOG_SOURCE, "No data found converting customer data for URL parameters.");
			return "";
		}

		final StringBuilder urlVars = new StringBuilder(1024);

		for (Map.Entry<String, String> entry : data.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();

			// check to make sure neither of our entry values is null or empty
			if (StringUtils.isNullOrEmpty(key) || StringUtils.isNullOrEmpty(value)) {
				continue;
			}

			// the first variable should have no '&' character,
			// but subsequent variables appended to this string should
			if (urlVars.length() != 0) {
				urlVars.append("&");
			}

			urlVars
				.append(AudienceConstants.AUDIENCE_MANAGER_CUSTOMER_DATA_PREFIX)
				.append(UrlUtils.urlEncode(key.replace(".", "_")))
				.append("=")
				.append(UrlUtils.urlEncode(value));
		}

		return urlVars.toString();
	}

	/**
	 * Generates URL parameters that represent Identity, UUID, and Data Provider variables
	 *
	 * @param event {@link Event} instance
	 * @return {@link String} value of Data Provider and Identity variables
	 */
	private String getDataProviderUrlVariables(final Event event) {
		final SharedStateResult identityResult = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Identity.MODULE_NAME,
			event
		);
		final Map<String, Object> identitySharedState = identityResult != null ? identityResult.getValue() : null;
		final SharedStateResult configResult = getSharedStateForExtension(
			AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
			event
		);
		final Map<String, Object> configurationSharedState = configResult != null ? configResult.getValue() : null;

		final StringBuilder urlVars = new StringBuilder(1024);

		if (identitySharedState != null) {
			final String marketingCloudId = DataReader.optString(
				identitySharedState,
				AudienceConstants.EventDataKeys.Identity.VISITOR_ID_MID,
				null
			);
			final String blob = DataReader.optString(
				identitySharedState,
				AudienceConstants.EventDataKeys.Identity.VISITOR_ID_BLOB,
				null
			);
			final String locationHint = DataReader.optString(
				identitySharedState,
				AudienceConstants.EventDataKeys.Identity.VISITOR_ID_LOCATION_HINT,
				null
			);

			// append mid
			if (!StringUtils.isNullOrEmpty(marketingCloudId)) {
				urlVars.append(serializeKeyValuePair(AudienceConstants.VISITOR_ID_MID_KEY, marketingCloudId));
			}

			// append blob
			if (!StringUtils.isNullOrEmpty(blob)) {
				urlVars.append(serializeKeyValuePair(AudienceConstants.VISITOR_ID_BLOB_KEY, blob));
			}

			// append location hint
			if (!StringUtils.isNullOrEmpty(locationHint)) {
				urlVars.append(serializeKeyValuePair(AudienceConstants.VISITOR_ID_LOCATION_HINT_KEY, locationHint));
			}

			// append customer Ids
			List<Map<String, Object>> customerIds = DataReader.optTypedListOfMap(
				Object.class,
				identitySharedState,
				AudienceConstants.EventDataKeys.Identity.VISITOR_IDS_LIST,
				null
			);

			String customerIdString = generateCustomerVisitorIdString(customerIds);

			if (!StringUtils.isNullOrEmpty(customerIdString)) {
				urlVars.append(customerIdString);
			}
		}

		if (configurationSharedState != null) {
			final String marketingCloudOrgId = DataReader.optString(
				configurationSharedState,
				AudienceConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
				null
			);

			// append orgId
			if (!StringUtils.isNullOrEmpty(marketingCloudOrgId)) {
				urlVars.append(serializeKeyValuePair(AudienceConstants.MARKETING_CLOUD_ORG_ID, marketingCloudOrgId));
			}
		}

		if (internalState != null) {
			// if we have a uuid, we should send it in the hit
			final String uuid = internalState.getUuid();

			if (!StringUtils.isNullOrEmpty(uuid)) {
				urlVars.append(serializeKeyValuePair(AudienceConstants.AUDIENCE_MANAGER_USER_ID_KEY, uuid));
			}
		}

		// remove leading '&' if we have a query string
		return urlVars.length() > 0 ? urlVars.substring(1) : "";
	}

	/**
	 * Generates customer VisitorID string.
	 * <p>
	 * The format of customer VisitorID string is:
	 * {@code &d_cid_ic=[customerIDType]%01[customerID]%01[authStateIntegerValue]}
	 * or, if {@code VisitorID.id} is not present
	 * {@code &d_cid_ic=[customerIDType]%01[authStateIntegerValue]}
	 * <p>
	 * Example: If {@code VisitorID.idType} is "id_type1", {@code VisitorID#id} is "id1" and {@code VisitorID.authenticationState}
	 * is {@code VisitorID.AuthenticationState#AUTHENTICATED} then generated customer id string
	 * shall be {@literal &d_cid_ic=id_type1%01id1%011}
	 *
	 * @param customerIds list of all the customer provided {@code VisitorID} as Map, obtained from the Identity shared state
	 * @return {@link String} containing URL encoded value of all the customer ids in the predefined format.
	 */
	private String generateCustomerVisitorIdString(final List<Map<String, Object>> customerIds) {
		if (customerIds == null) {
			return null;
		}

		final StringBuilder customerIdString = new StringBuilder();

		for (Map<String, Object> visitorId : customerIds) {
			if (visitorId != null) {
				customerIdString.append(
					serializeKeyValuePair(
						AudienceConstants.VISITOR_ID_PARAMETER_KEY_CUSTOMER,
						DataReader.optString(
							visitorId,
							AudienceConstants.EventDataKeys.Identity.VisitorID.ID_TYPE,
							null
						)
					)
				);

				String urlEncodedId = UrlUtils.urlEncode(
					DataReader.optString(visitorId, AudienceConstants.EventDataKeys.Identity.VisitorID.ID, null)
				);

				if (!StringUtils.isNullOrEmpty(urlEncodedId)) {
					customerIdString.append(AudienceConstants.VISITOR_ID_CID_DELIMITER);
					customerIdString.append(urlEncodedId);
				}

				customerIdString.append(AudienceConstants.VISITOR_ID_CID_DELIMITER);
				customerIdString.append(
					DataReader.optInt(visitorId, AudienceConstants.EventDataKeys.Identity.VisitorID.STATE, 0)
				); // default authentication unknown
			}
		}

		return customerIdString.toString();
	}

	/**
	 * Generates a URL suffix for AAM requests containing platform information.
	 * <p>
	 * Returns suffix with generic platform name "java" if the canonical platform name is
	 * unavailable from the {@link ServiceProvider}.
	 *
	 * @return {@link String} representing the URL suffix for AAM request
	 */
	private String getPlatformSuffix() {
		String platform = "java";
		DeviceInforming deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
		if (deviceInfoService == null) {
			return AudienceConstants.AUDIENCE_MANAGER_URL_PLATFORM_KEY + platform;
		}

		final String canonicalPlatformName = deviceInfoService.getCanonicalPlatformName();
		if (!StringUtils.isNullOrEmpty(canonicalPlatformName)) {
			platform = canonicalPlatformName;
		}
		return AudienceConstants.AUDIENCE_MANAGER_URL_PLATFORM_KEY + platform;
	}

	/**
	 * Loops through the "dests" array of an AAM response and attempts to forward requests where necessary.
	 *
	 * @param jsonResponse the {@link JSONObject} representation of the AAM server response
	 * @param timeout {@code int} indicating connection timeout value for requests
	 */
	private void processDestsArray(final JSONObject jsonResponse, final int timeout) {
		try {
			// check "dests" for urls to send
			final JSONArray dests = jsonResponse.getJSONArray(AudienceConstants.AUDIENCE_MANAGER_JSON_DESTS_KEY);

			for (int i = 0; i < dests.length(); i++) {
				final JSONObject dest = dests.getJSONObject(i);

				if (dest.length() == 0) {
					continue;
				}

				final String url = dest.optString(AudienceConstants.AUDIENCE_MANAGER_JSON_URL_KEY, "");

				if (!StringUtils.isNullOrEmpty(url)) {
					final NetworkRequest request = new NetworkRequest(
						url,
						HttpMethod.GET,
						null,
						null,
						timeout,
						timeout
					);
					ServiceProvider
						.getInstance()
						.getNetworkService()
						.connectAsync(
							request,
							connection -> {
								if (connection == null) {
									return;
								}

								if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
									Log.trace(LOG_TAG, LOG_SOURCE, "Successfully forwarded 'dest'.");
								} else {
									Log.trace(
										LOG_TAG,
										LOG_SOURCE,
										"Failed to process dest - connection status \"%s\".",
										connection.getResponseCode()
									);
								}

								connection.close();
							}
						);
				}
			}
		} catch (final JSONException ex) {
			Log.trace(LOG_TAG, LOG_SOURCE, "No destinations ('dests') in response.");
		}
	}

	/**
	 * Loops through the "stuff" array of an AAM response and creates a {@code Map} representing the segments for the user.
	 *
	 * @param jsonResponse the {@link JSONObject} representation of the AAM server response
	 * @return a {@code Map<String, String>} representing the segments for the user
	 */
	private @NonNull Map<String, String> processStuffArray(final JSONObject jsonResponse) {
		final Map<String, String> returnedMap = new HashMap<>();

		try {
			final JSONArray stuffArray = jsonResponse.getJSONArray(AudienceConstants.AUDIENCE_MANAGER_JSON_STUFF_KEY);

			// loop through array and make a more user friendly dictionary
			for (int i = 0; i < stuffArray.length(); i++) {
				final JSONObject stuff = stuffArray.getJSONObject(i);

				if (stuff != null && stuff.length() != 0) {
					final String cookieName = stuff.optString(
						AudienceConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY,
						""
					);
					final String cookieValue = stuff.optString(
						AudienceConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY,
						""
					);

					if (!cookieName.isEmpty()) {
						returnedMap.put(cookieName, cookieValue);
					}
				}
			}
		} catch (final JSONException ex) {
			Log.trace(LOG_TAG, LOG_SOURCE, "No 'stuff' array in response.");
		}

		return returnedMap;
	}

	/**
	 * Serializes a key/value pair for URL consumption, e.g. {@code &key=value}.
	 * <p>
	 * It returns null if the key or the value is null or empty.
	 *
	 * @param key nullable {@link String} to be appended before "="
	 * @param value nullable {@code String} to be appended after "="
	 * @return nullable serialized key-value pair
	 */
	String serializeKeyValuePair(final String key, final String value) {
		if (StringUtils.isNullOrEmpty(key) || value == null) {
			return null;
		}

		return "&" + key + "=" + value;
	}
}
