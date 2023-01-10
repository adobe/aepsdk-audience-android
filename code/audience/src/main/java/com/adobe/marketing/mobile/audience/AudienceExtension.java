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

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceConstants.EX
import static com.adobe.marketing.mobile.audience.AudienceConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.audience.AudienceConstants.FRIENDLY_EXTENSION_NAME;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.HttpURLConnection;

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
 *   <li>{@code EventType.AUDIENCEMANAGER} - {@link EventSource#REQUEST_IDENTITY}</li>
 *   <li>{@code EventType.AUDIENCEMANAGER} - {@link EventSource#REQUEST_RESET}</li>
 *   <li>{@link EventType#CONFIGURATION} - {@code EventSource.RESPONSE_CONTENT}</li>
 *   <li>{@link EventType#HUB} - {@link EventSource#SHARED_STATE}</li>
 *   <li>{@link EventType#LIFECYCLE} - {@code EventSource.RESPONSE_CONTENT}</li>
 *   <li>{@link EventType#RULES_ENGINE} - {@code EventSource.REQUEST_CONTENT}</li>
 *</ol>
 *
 * The AudienceExtension dispatches the following {@code Event}s:
 * <ol>
 *   <li>{@code EventType.AUDIENCEMANAGER} - {@code EventSource.RESPONSE_CONTENT}</li>
 *   <li>{@code EventType.AUDIENCEMANAGER} - {@link EventSource#RESPONSE_IDENTITY}</li>
 * </ol>
 *
 * The AudienceExtension has dependencies on the following {@link PlatformServices}:
 * <ol>
 *   <li>{@link LocalStorageService}</li>
 *   <li>{@link DatabaseService}</li>
 *   <li>{@link JsonUtilityService}</li>
 *   <li>{@link NetworkService}</li>
 * </ol>
 */
public final class AudienceExtension extends Extension {
	private static final String LOG_TAG = "Audience";
	private static final String CLASS_NAME = AudienceExtension.class.getSimpleName();

	private AudienceState internalState = null;
	private AudienceHitsDatabase internalDatabase = null;

	/**
	 * Constructor.
	 * <p>
	 * Creates the necessary dispatchers and registers listeners with the {@link EventHub}.
	 *
	 * @param hub an {@code EventHub} instance to be used by the extension
	 * @param services an instance of {@link PlatformServices}
	 */
	AudienceExtension(final ExtensionApi extensionApi) {
		this(extensionApi, getState(), null, null);
	}

	//@VisibileForTesting
	AudienceExtension(final ExtensionApi extensionApi, final AudienceState audienceState, final AudienceHitsDatabase audienceHitsDatabase) {
		super(extensionApi);

		this.internalState = audienceState != null ? audienceState : getState();
		this.internalDatabase = audienceHitsDatabase != null ? audienceHitsDatabase : new AudienceHitsDatabase(this, services);
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
		return Audience.extensionVersion();
	}

	@Override
	protected void onRegistered() {
		getApi().registerEventListener(EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT, this::handleEvent);

		registerListener(EventType.AUDIENCEMANAGER, EventSource.REQUEST_IDENTITY,
				ListenerAudienceRequestIdentityAudienceManager.class);
		registerListener(EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET,
				ListenerAudienceRequestResetAudienceManager.class);
		registerListener(EventType.ANALYTICS, EventSource.RESPONSE_CONTENT,
				ListenerAnalyticsResponseContentAudienceManager.class);
		registerListener(EventType.HUB, EventSource.SHARED_STATE, ListenerHubSharedStateAudienceManager.class);
		registerListener(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT,
				ListenerLifecycleResponseContentAudienceManager.class);
		registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT,
				ListenerConfigurationResponseContentAudienceManager.class);

		Log.trace(LOG_TAG, CLASS_NAME, "Dispatching Audience shared state");
		saveAamStateForVersion(0);
	}

	@Override
	protected void onUnregistered() {}

	@Override
	public boolean readyForEvent(@NonNull final Event event) {
		if (!hasValidSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event)) {
			Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid Configuration");
			return false;
		}

		if (!hasValidSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
			Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid XDM shared state from Edge Identity extension.");
			return false;
		}

		// fetch in-app messages on initial launch once we have configuration and identity state set
		if (!initialMessageFetchComplete) {
			inAppNotificationHandler.fetchMessages();
			initialMessageFetchComplete = true;
		}

		return true;
	}

	//endregion

	//region Event listeners

	// audience response content
	void handleEvent(final Event event) {
		// validate input
		if (event == null) {
			Log.warning(LOG_TAG, "queueAamEvent - Unable to queue event as event is null");
			return;
		}

		final AudienceState state = getState();

		if (state == null) {
			Log.warning(LOG_TAG, "queueAamEvent - Unable to queue event as state is null");
			return;
		}

		// if current privacy status is OPT_OUT, fast fail here
		if (state.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT) {
			dispatchPairedIdResponseIfNecessary(Collections.<String, String>emptyMap(), event);
			Log.trace(LOG_TAG, "queueAamEvent - Unable to process AAM event as privacy status is optedout: %s", event);
			return;
		}

		// add current event to the queue
		waitingEvents.add(event);

		// try to process queued events
		Log.trace(LOG_TAG, "queueAamEvent - try to process queued events: %s", event);
		processQueuedEvents();
	}

	// analytics request content
	void processResponse(final Event event) {

	}

	void handleIdentityRequest(final Event event) {

	}


	//endregion

	// ========================================================
	// package-protected methods
	// ========================================================

	/**
	 * Invokes the dispatcher passing the current DPID, DPUUID and visitor profile to dispatch {@code AUDIENCEMANAGER},
	 * {@code RESPONSE_IDENTITY} event.
	 * <p>
	 * If the {@code pairId} is empty or null, dispatcher is not invoked.
	 *
	 * @param pairId Optional {@link String} containing the {@code pairId} for this request
	 */
	void getIdentityVariables(final String pairId) {
		final AudienceState state = getState();

		if (state == null) {
			Log.warning(LOG_TAG, "getIdentityVariables - Not able to get Identity Variables as state is null");
			return;
		}

		if (!StringUtils.isNullOrEmpty(pairId)) {
			dispatcherAudienceResponseIdentity.dispatch(state.getVisitorProfile(), state.getDpid(), state.getDpuuid(),
					pairId);
			Log.trace(LOG_TAG, "getIdentityVariables - getting Identity Variables");
		}
	}


	/**
	 * Initialize and get the AudienceHitsDatabase object
	 *
	 * @return {@code AudienceHitsDatabase} object of AudienceHitsDatabase
	 */
	private AudienceHitsDatabase getDatabase() {
		PlatformServices  services = getPlatformServices();

		if (internalDatabase == null && services != null) {
			internalDatabase = new AudienceHitsDatabase(AudienceExtension.this, services);
		}

		Log.trace(LOG_TAG, "getDatabase - Get internal Audience Hit database");
		return internalDatabase;
	}

	/**
	 * Initialize and get the AudienceState object
	 *
	 * @return {@code AudienceHitsDatabase} object of AudienceState
	 */
	private AudienceState getState() {
		PlatformServices  services = getPlatformServices();

		if (internalState == null && services != null) {
			internalState = new AudienceState(services.getLocalStorageService());
		}

		Log.trace(LOG_TAG, "getState - Get internal Audience State");
		return internalState;
	}


	/**
	 * Called when there is a shared state change.
	 *
	 * @param stateName {@link String} identifying the name of the state that changed
	 */
	void processStateChange(final String stateName) {
		// submitting signals requires waiting on configuration and potentially on identity
		if (AudienceConstants.EventDataKeys.Configuration.MODULE_NAME.equalsIgnoreCase(stateName) ||
				AudienceConstants.EventDataKeys.Identity.MODULE_NAME.equalsIgnoreCase(stateName)) {
			processQueuedEvents();
		}
	}

	/**
	 * Dispatches shared state update upon {@link EventHub} boot completion
	 *
	 * @param bootEvent {@link Event} of boot completion event
	 */
	void bootup(final Event bootEvent) {

	}


	/**
	 * Adds an {@code Event} object to the {@link #waitingEvents} queue to be handled by this extension.
	 *
	 * @param event {@link Event} instance to be processed
	 */
	void queueAamEvent(final Event event) {
		// validate input
		if (event == null) {
			Log.warning(LOG_TAG, "queueAamEvent - Unable to queue event as event is null");
			return;
		}

		final AudienceState state = getState();

		if (state == null) {
			Log.warning(LOG_TAG, "queueAamEvent - Unable to queue event as state is null");
			return;
		}

		// if current privacy status is OPT_OUT, fast fail here
		if (state.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT) {
			dispatchPairedIdResponseIfNecessary(Collections.<String, String>emptyMap(), event);
			Log.trace(LOG_TAG, "queueAamEvent - Unable to process AAM event as privacy status is optedout: %s", event);
			return;
		}

		// add current event to the queue
		waitingEvents.add(event);

		// try to process queued events
		Log.trace(LOG_TAG, "queueAamEvent - try to process queued events: %s", event);
		processQueuedEvents();
	}

	/**
	 * Resets UUID, DPID, DPUUID, and the Audience Manager Visitor Profile from the {@code AudienceState} instance.
	 * <p>
	 * This method also updates shared state for version provided by {@code event} param.
	 *
	 * @param event {@link Event} containing shared state version information to be updated
	 */
	void reset(final Event event) {

		final AudienceState state = getState();

		if (event == null || state == null) {
			Log.warning(LOG_TAG, "reset - No event can be reset");
			return;
		}

		state.clearIdentifiers();

		saveAamStateForVersion(event.getEventNumber());
	}

	/**
	 * Process the configuration {@code EventSource.RESPONSE_CONTENT} event.
	 *
	 * <p>
	 *
	 * This event contains the configuration data. If the {@link AudienceConstants.EventDataKeys.Configuration#GLOBAL_CONFIG_PRIVACY} value is
	 * {@link MobilePrivacyStatus#OPT_OUT} then we send out an opt-out hit to the Audience manager server, along with resetting the Audience Manager Ids.
	 *
	 * @param event  The {@link Event} received by the {@link ListenerConfigurationResponseContentAudienceManager}
	 *
	 * @see #reset(Event)
	 * @see #sendOptOutHit(EventData)
	 *
	 */
	void processConfiguration(final Event event) {
		final AudienceState state = getState();

		if (event == null || state == null) {
			Log.warning(LOG_TAG, "processConfiguration - No event can be processed");
			return;
		}

		final EventData configuration = event.getData();
		final AudienceHitsDatabase database = getDatabase();


		if (configuration == null) {
			Log.warning(LOG_TAG, "processConfiguration - Not processing configuration as no configuration info.");
			return;
		}

		MobilePrivacyStatus mobilePrivacyStatus = MobilePrivacyStatus.
				fromString(configuration.optString(AudienceConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, ""));

		state.setMobilePrivacyStatus(mobilePrivacyStatus);

		if (mobilePrivacyStatus.equals(MobilePrivacyStatus.OPT_OUT)) {
			sendOptOutHit(configuration);
			reset(event); // reset() creates a shared state
			clearWaitingEvents();
		}

		if (database != null) {
			database.updatePrivacyStatus(mobilePrivacyStatus);
		} else {
			Log.warning(LOG_TAG, "processConfiguration - Audience Database not initialized. Unable to update privacy status");
		}

		processQueuedEvents();

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
	 * @param configuration The configuration {@link EventData} object
	 *
	 * @see DispatcherAudienceResponseContentAudienceManager#dispatchOptOutResult(boolean)
	 */
	private void sendOptOutHit(final EventData configuration) {
		NetworkService networkService = getPlatformServices().getNetworkService();
		final AudienceState state = getState();

		if (networkService == null || state == null) {
			Log.warning(LOG_TAG,
						"SendOptOutHit - Unable to send opt-out signal to Audience service, platform network service unavailable.");
			return;
		}

		//If opt-out, and we have a UUID, then send a optout hit, and dispatch an event
		final String aamServer = configuration.
								 optString(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, null);
		final String uuid = state.getUuid();

		boolean canSendOptOutHit = !StringUtils.isNullOrEmpty(aamServer) && !StringUtils.isNullOrEmpty(uuid);

		if (canSendOptOutHit) {
			//Send the opt-out hit
			String optOutUrl = getOptOutUrlPrefix(aamServer) + getOptOutUrlSuffix(uuid);

			final int timeout = configuration.optInteger(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
								AudienceConstants.DEFAULT_AAM_TIMEOUT);
			//We don't expect a response back. This is a fire and forget
			networkService.connectUrlAsync(optOutUrl,
										   NetworkService.HttpCommand.GET,
			null, null, timeout, timeout, new NetworkService.Callback() {
				@Override
				public void call(final NetworkService.HttpConnection connection) {
					if (connection == null) {
						return;
					}

					if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
						Log.trace(LOG_TAG, "sendOptOutHit - Successfully sent the optOut hit.");
					} else {
						Log.trace(LOG_TAG, "sendOptOutHit - Failed to send the optOut hit with connection status (%s).",
								  connection.getResponseCode());
					}

					connection.close();
				}
			});
		}

		dispatcherAudienceResponseContent.dispatchOptOutResult(canSendOptOutHit);
	}

	/**
	 * Sets the {@code dpid} and {@code dpuuid} in the {@code AudienceState} instance and updates shared state for
	 * {@code AudienceExtension}.
	 *
	 * @param dpid new {@code dpid} value
	 * @param dpuuid new {@code dpuuid} value
	 * @param event {@link Event} instance used to get version number for shared state
	 */
	void setDpidAndDpuuid(final String dpid, final String dpuuid, final Event event) {
		// without an event, we can't update shared state
		final AudienceState state = getState();

		if (event == null || state == null) {
			Log.warning(LOG_TAG, "setDpidAndDpuuid - No event can be set.");
			return;
		}

		state.setDpid(dpid);
		state.setDpuuid(dpuuid);
		Log.trace(LOG_TAG, "setDpidAndDpuuid - Audience set dpid and dpuuid state");
		saveAamStateForVersion(event.getEventNumber());
	}

	// ========================================================
	// protected methods
	// ========================================================

	/**
	 * Loops through the existing list of {@code waitingEvents} and processes them.
	 * <p>
	 * Processing involves invoking {@link #submitSignal(Event)} passing the current {@code event}, if the {@code event} qualifies.
	 * Once processed, events are popped from the {@link #waitingEvents} queue.
	 * <p>
	 * Events are not processed if {@code Configuration} shared state is unavailable or {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER}
	 * is not configured or if {@code waitingEvents} is empty.
	 */
	protected void processQueuedEvents() {
		// process all of our waiting events if we can
		while (!waitingEvents.isEmpty()) {
			// get the top event from our list
			final Event currentEvent = waitingEvents.peek();

			if (currentEvent == null) {
				Log.warning(LOG_TAG, "ProcessQueuedEvents - Stopped processing as no current event");
				break;
			}

			// make sure we have configuration and it contains values we need for Audience Manager
			final EventData configuration = getSharedEventState(AudienceConstants.EventDataKeys.Configuration.MODULE_NAME,
											currentEvent);

			if (configuration == EventHub.SHARED_STATE_PENDING) {
				Log.warning(LOG_TAG, "ProcessQueuedEvents - Stopped processing as the shared state is pending");
				break;
			}

			final String server = configuration.optString(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, null);

			if (StringUtils.isNullOrEmpty(server)) {
				Log.warning(LOG_TAG, "ProcessQueuedEvents - Stopped processing as no Audience Server in config");
				break;
			}

			// an Identity state is a soft dependency, however if an Identity state is pending, wait for it
			final EventData identityState = getSharedEventState(AudienceConstants.EventDataKeys.Identity.MODULE_NAME, currentEvent);

			// a PENDING state may either mean a new state will arrive or the extension has not shared any state
			// if Identity has not shared any state, continue to process the event without the Identity state
			// however, if an Identity is truly pending, break and process this event later
			if (identityState == EventHub.SHARED_STATE_PENDING
					&& hasSharedEventState(AudienceConstants.EventDataKeys.Identity.MODULE_NAME)) {
				Log.warning(LOG_TAG, "ProcessQueuedEvents - Stopped processing as Identity shared state is pending");
				break;
			}

			// if this is an aam event or a lifecycle event and aam forwarding is disabled, process the event
			// lifecycle events are handled differently than regular aam events
			if (currentEvent.getEventType() == EventType.AUDIENCEMANAGER) {
				submitSignal(currentEvent);
			} else if (currentEvent.getEventType() == EventType.LIFECYCLE
					   && !configuration.optBoolean(AudienceConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING, false)) {
				final EventData eventData = currentEvent.getData();

				if (eventData != null) {
					final HashMap<String, String> tempLifecycleData = (HashMap<String, String>)eventData.optStringMap(
								AudienceConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, null);

					final HashMap<String, String> lifecycleContextData = getLifecycleDataForAudience(tempLifecycleData);

					final EventData newEventData = new EventData();
					newEventData.putStringMap(AudienceConstants.EventDataKeys.Audience.VISITOR_TRAITS, lifecycleContextData);

					final Event newCurrentEvent = new Event.Builder("Audience Manager Profile", EventType.AUDIENCEMANAGER,
							EventSource.RESPONSE_CONTENT).setData(newEventData).setTimestamp(currentEvent.getTimestamp())
					.setEventNumber(currentEvent.getEventNumber()).build();
					submitSignal(newCurrentEvent);
				}
			}

			// pop the current event
			waitingEvents.poll();
		}
	}

	/**
	 * Converts data from a lifecycle event into its form desired by Audience Manager.
	 *
	 * @param lifecycleData map containing {@link EventData} from a {@code Lifecycle} {@link Event}
	 * @return {@code HashMap<String, String>} containing Lifecycle data transformed for Audience Manager
	 */
	private HashMap<String, String> getLifecycleDataForAudience(final HashMap<String, String> lifecycleData) {
		final HashMap<String, String> lifecycleContextData = new HashMap<String, String>();

		if (lifecycleData == null || lifecycleData.isEmpty()) {
			return lifecycleContextData;
		}

		// copy the event's data so we don't accidentally overwrite it for someone else consuming this event
		final Map<String, String> tempLifecycleContextData = new HashMap<String, String>(lifecycleData);

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
	protected Map<String, String> processResponse(final String response, final Event event) {
		// bail out early if we receive a empty response
		PlatformServices platformServices;
		JsonUtilityService jsonUtilityService;

		if (StringUtils.isNullOrEmpty(response)) {
			Log.warning(LOG_TAG, "processResponse - Failed to read server response");
			return null;
		}

		// get timeout from config
		final EventData configData = getSharedEventState(AudienceConstants.EventDataKeys.Configuration.MODULE_NAME, event);
		final AudienceState state = getState();

		if (state == null) {
			return null;
		}

		if (configData == EventHub.SHARED_STATE_PENDING) {
			return null;
		}

		final int timeout = configData.optInteger(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
							AudienceConstants.DEFAULT_AAM_TIMEOUT);

		platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.warning(LOG_TAG, "processResponse - Platform services are not available");
			return null;
		}

		jsonUtilityService = platformServices.getJsonUtilityService();

		if (jsonUtilityService == null) {
			Log.warning(LOG_TAG, "processResponse - JSON services are not available");
			return null;
		}

		final JSONObject jsonResponse = jsonUtilityService.createJSONObject(response);

		// Bail out if there were any error occurred during the parsing of JSON
		if (jsonResponse == null) {
			return null;
		}

		// check "dests" for urls to send
		processDestsArray(jsonResponse, timeout);

		try {
			// save uuid for use with subsequent calls
			// Note, the AudienceState may have a different privacy status than that of the calling event.
			// Setting the UUID may fail if the AudienceState's current privacy is opt-out
			state.setUuid(jsonResponse.getString(AudienceConstants.AUDIENCE_MANAGER_JSON_USER_ID_KEY));
		} catch (final JsonException ex) {
			Log.debug(LOG_TAG, "processResponse - Unable to retrieve UUID from Audience Manager response (%s)",
					  ex);
		}

		// process the "stuff" array
		final Map<String, String> returnedMap = processStuffArray(jsonResponse);

		if (returnedMap.size() > 0) {
			Log.trace(LOG_TAG, "processResponse - Response received (%s)", returnedMap);
		} else {
			Log.trace(LOG_TAG, "processResponse - Response was empty");
		}

		// save profile in defaults
		// Note, the AudienceState may have a different privacy status than that of the calling event.
		// Setting the visitor profile may fail if the AudienceState's current privacy is opt-out
		state.setVisitorProfile(returnedMap);

		saveAamStateForVersion(event.getEventNumber());

		return returnedMap;
	}

	/**
	 * Sends a signal to the Audience Manager server.
	 * <p>
	 * A signal is not submitted to Audience Manager if,
	 * <ul>
	 *     <li>{@code Configuration} shared state is unavailable.</li>
	 *     <li>{@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER} is not configured and is null or empty.</li>
	 *     <li>{@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#GLOBAL_CONFIG_PRIVACY} is set to {@link MobilePrivacyStatus#OPT_OUT}.</li>
	 * </ul>
	 * Additionally, if privacy status is not set and is {@link MobilePrivacyStatus#UNKNOWN}, then signal hits are queued until user opts in or out.
	 *
	 * @param event {@link Event} containing data to be sent to the Audience Manager.
	 */
	protected void submitSignal(final Event event) {
		// make sure we have configuration first
		final EventData configData = getSharedEventState(AudienceConstants.EventDataKeys.Configuration.MODULE_NAME, event);
		final AudienceHitsDatabase database = getDatabase();

		if (configData == EventHub.SHARED_STATE_PENDING) {
			// intentionally not calling the callback here, because this method shouldn't be called before
			// we know we have config
			return;
		}

		final String server = configData.optString(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, null);
		final int timeout = configData.optInteger(AudienceConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT,
							AudienceConstants.DEFAULT_AAM_TIMEOUT);
		final MobilePrivacyStatus privacyStatus = MobilePrivacyStatus.fromString(
					configData.optString(AudienceConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY,
										 MobilePrivacyStatus.UNKNOWN.getValue()));

		// make sure we have configuration before we move on
		if (StringUtils.isNullOrEmpty(server) || privacyStatus == MobilePrivacyStatus.OPT_OUT) {
			// create an empty valid shared state if privacy is opt-out.
			// if not configured, dispatch an empty event for the pairId (if necessary)
			Log.debug(LOG_TAG, "submitSignal - Dispatch an empty profile as privacy is opt-out");
			dispatchPairedIdResponseIfNecessary(null, event);
			return;
		}


		if (privacyStatus == MobilePrivacyStatus.UNKNOWN) {
			// when privacy opt-unknown, callback should be immediately called with empty data.
			Log.debug(LOG_TAG, "submitSignal - Dispatch an empty profile as privacy is unknown");
			dispatchPairedIdResponseIfNecessary(null, event);
		}

		if (database == null) {
			Log.warning(LOG_TAG, "submitSignal - Unable to queue AAM request as Audience Database not initialized.");
			return;
		}

		// generate the url to send
		final String requestUrl = buildSignalUrl(server, event);
		Log.debug(LOG_TAG, "submitSignal - Queuing request - %s", requestUrl);
		database.queue(requestUrl, timeout, privacyStatus, event);
	}

	/**
	 * This method is called by the {@code AudienceHitsDatabase} after a network request has been processed.
	 *
	 * @param response {@link String} representation of the response from the AAM server
	 * @param event triggering {@link Event} that caused the AAM request
	 */
	void handleNetworkResponse(final String response, final Event event) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				if (event == null) {
					Log.warning(LOG_TAG, "handleNetworkResponse - Unable to process network response, invalid event.");
					return;
				}

				Map<String, String> profile = new HashMap<String, String>();

				if (StringUtils.isNullOrEmpty(response)) {
					Log.warning(LOG_TAG, "handleNetworkResponse - No response from server.");
					dispatchPairedIdResponseIfNecessary(profile, event);
					saveAamStateForVersion(event.getEventNumber());
					return;
				}

				// process the response from the AAM server
				profile = processResponse(response, event);

				// if profile is empty, there was a json error in the response, don't dispatch a generic event
				if (profile != null && !profile.isEmpty()) {
					dispatcherAudienceResponseContent.dispatch(profile, null);
				}

				// dispatch to one-time listener if we have one
				dispatchPairedIdResponseIfNecessary(profile, event);
			}
		});

	}

	// ========================================================
	// private methods
	// ========================================================

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
		final String urlPrefix = getUrlPrefix(server);

		// get traits from event
		final EventData customerEventData = event.getData();
		final Map<String, String> customerData = customerEventData == null ? null :
				customerEventData.optStringMap(AudienceConstants.EventDataKeys.Audience.VISITOR_TRAITS, null);
		String urlString = urlPrefix + getCustomUrlVariables(customerData) + getDataProviderUrlVariables(event)
						   + getPlatformSuffix() + AudienceConstants.AUDIENCE_MANAGER_URL_SUFFIX;

		return urlString.replace("?&", "?");
	}

	/**
	 * Invokes the dispatcher passing the current visitor {@code profile} and {@code event} to dispatch {@code AUDIENCEMANAGER},
	 * {@code RESPONSE_CONTENT} event to paired id listener if the {@code event} has one.
	 *
	 * @param profile {@code Map<String, String>} containing the user's profile
	 * @param event {@link Event} object that may or may not contain a {@code pairId}
	 */
	private void dispatchPairedIdResponseIfNecessary(final Map<String, String> profile, final Event event) {
		if (StringUtils.isNullOrEmpty(event.getResponsePairID())) {
			Log.warning(LOG_TAG, "dispatchPairedIdResponseIfNecessary - Response pair id is not available.");
			return;
		}

		dispatcherAudienceResponseContent.dispatch(profile, event.getResponsePairID());
	}

	/**
	 * Processes the provided map of customer data and converts them for use as URL parameters.
	 *
	 * @param data {@code Map<String, String>} of customer data to be converted
	 * @return {@link String} representing value of URL parameters
	 */
	private String getCustomUrlVariables(final Map<String, String> data) {
		if (data == null || data.size() == 0) {
			Log.warning(LOG_TAG, "dispatchPairedIdResponseIfNecessary - No data is found.");

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

			if (value.getClass() == String.class) {
				urlVars.append("&")
				.append(AudienceConstants.AUDIENCE_MANAGER_CUSTOMER_DATA_PREFIX)
				.append(UrlUtilities.urlEncode(sanitizeUrlVariableName(key)))
				.append("=")
				.append(UrlUtilities.urlEncode(value));
			}
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
		final EventData visitorIdState = getSharedEventState(AudienceConstants.EventDataKeys.Identity.MODULE_NAME, event);
		final EventData configData = getSharedEventState(AudienceConstants.EventDataKeys.Configuration.MODULE_NAME, event);
		final AudienceState state = getState();

		final StringBuilder urlVars = new StringBuilder(1024);

		if (visitorIdState != null) {
			final String marketingCloudId = visitorIdState.optString(AudienceConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);
			final String blob = visitorIdState.optString(AudienceConstants.EventDataKeys.Identity.VISITOR_ID_BLOB, null);
			final String locationHint = visitorIdState.optString(AudienceConstants.EventDataKeys.Identity.VISITOR_ID_LOCATION_HINT,
										null);

			// append mid
			if (!StringUtils.isNullOrEmpty(marketingCloudId)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.VISITOR_ID_MID_KEY, marketingCloudId));
			}

			// append blob
			if (!StringUtils.isNullOrEmpty(blob)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.VISITOR_ID_BLOB_KEY, blob));
			}

			// append location hint
			if (!StringUtils.isNullOrEmpty(locationHint)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.VISITOR_ID_LOCATION_HINT_KEY, locationHint));
			}

			// append customer Ids
			List<VisitorID> customerIds = visitorIdState.optTypedList(AudienceConstants.EventDataKeys.Identity.VISITOR_IDS_LIST,
										  new ArrayList<VisitorID>(), VisitorID.VARIANT_SERIALIZER);

			String customerIdString = generateCustomerVisitorIdString(customerIds);

			if (!StringUtils.isNullOrEmpty(customerIdString)) {
				urlVars.append(customerIdString);
			}
		}

		if (configData != null) {
			final String marketingCloudOrgId = configData.optString(
												   AudienceConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID, null);

			// append orgId
			if (!StringUtils.isNullOrEmpty(marketingCloudOrgId)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.MARKETING_CLOUD_ORG_ID, marketingCloudOrgId));
			}
		}

		if (state != null) {
			// if we have a uuid, we should send it in the hit
			final String uuid = state.getUuid();

			if (!StringUtils.isNullOrEmpty(uuid)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.AUDIENCE_MANAGER_USER_ID_KEY, uuid));
			}

			// append dpid and dpuuid IFF both are available
			final String dpid = state.getDpid();
			final String dpuuid = state.getDpuuid();

			if (!StringUtils.isNullOrEmpty(dpid) && !StringUtils.isNullOrEmpty(dpuuid)) {
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.AUDIENCE_MANAGER_DATA_PROVIDER_ID_KEY, dpid));
				urlVars.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.AUDIENCE_MANAGER_DATA_PROVIDER_USER_ID_KEY,
							   dpuuid));
			}
		}

		return urlVars.toString();
	}

	/**
	 * Generates Customer VisitorID String.
	 * <p>
	 * The format of customer VisitorID string is:
	 * {@code &d_cid_ic=[customerIDType]%01[customerID]%01[authStateIntegerValue]}
	 * or, if {@link VisitorID#id} is not present
	 * {@code &d_cid_ic=[customerIDType]%01[authStateIntegerValue]}
	 * <p>
	 * Example: If {@link VisitorID#idType} is "id_type1", {@code VisitorID#id} is "id1" and {@link VisitorID#authenticationState}
	 * is {@link VisitorID.AuthenticationState#AUTHENTICATED} then generated customer id string
	 * shall be {@literal &d_cid_ic=id_type1%01id1%011}
	 *
	 * @param customerIds list of all the customer provided {@code VisitorID} objects obtained from the Identity shared state
	 * @return {@link String} containing URL encoded value of all the customer ids in the predefined format.
	 */
	private String generateCustomerVisitorIdString(final List<VisitorID> customerIds) {
		if (customerIds == null) {
			return null;
		}

		final StringBuilder customerIdString = new StringBuilder();

		for (VisitorID visitorId : customerIds) {
			if (visitorId != null) {
				customerIdString.append(UrlUtilities.serializeKeyValuePair(AudienceConstants.VISITOR_ID_PARAMETER_KEY_CUSTOMER,
										visitorId.getIdType()));

				String urlEncodedId = UrlUtilities.urlEncode(visitorId.getId());

				if (!StringUtils.isNullOrEmpty(urlEncodedId)) {
					customerIdString.append(AudienceConstants.VISITOR_ID_CID_DELIMITER);
					customerIdString.append(urlEncodedId);
				}

				customerIdString.append(AudienceConstants.VISITOR_ID_CID_DELIMITER);
				customerIdString.append(visitorId.getAuthenticationState().getValue());
			}
		}

		return customerIdString.toString();
	}

	/**
	 * Generates a URL suffix for AAM requests containing platform information.
	 * <p>
	 * Returns suffix with generic platform name "java" if {@link SystemInfoService} is not initialized or
	 * when the {@link SystemInfoService#getCanonicalPlatformName()} returns empty or null.
	 *
	 * @return {@link String} representing the URL suffix for AAM request
	 */
	private String getPlatformSuffix() {
		String platform = AudienceConstants.AUDIENCE_MANAGER_URL_PLATFORM_KEY + "java";

		if (getPlatformServices() == null) {
			Log.warning(LOG_TAG, "getPlatformSuffix - Platform services are not available");
			return platform;
		}

		SystemInfoService systemInfoService = getPlatformServices().getSystemInfoService();

		if (systemInfoService != null && !StringUtils.isNullOrEmpty(systemInfoService.getCanonicalPlatformName())) {
			platform = AudienceConstants.AUDIENCE_MANAGER_URL_PLATFORM_KEY + systemInfoService.getCanonicalPlatformName();
		}

		return platform;
	}

	/**
	 * Generates a URL prefix used for creating requests sent to Audience Manager.
	 *
	 * @param server {@link String} containing server name
	 * @return {@link String} representing the URL prefix for Audience Manager requests
	 */
	private String getUrlPrefix(final String server) {
		return String.format("https://%s/event?", server);
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
	 * Loops through the "dests" array of an AAM response and attempts to forward requests where necessary.
	 *
	 * @param jsonResponse the {@link JsonUtilityService.JSONObject} representation of the AAM server response
	 * @param timeout {@code int} indicating connection timeout value for requests
	 */
	private void processDestsArray(final JSONObject jsonResponse, final int timeout) {
		try {
			// check "dests" for urls to send
			final JSONArray dests = jsonResponse.getJSONArray(AudienceConstants.AUDIENCE_MANAGER_JSON_DESTS_KEY);

			if (dests == null) {
				// missing the dests key
				return;
			}

			for (int i = 0; i < dests.length(); i++) {
				final JSONObject dest = dests.getJSONObject(i);

				if (dest.length() == 0) {
					continue;
				}

				final String url = dest.optString(AudienceConstants.AUDIENCE_MANAGER_JSON_URL_KEY, null);

				if (!StringUtils.isNullOrEmpty(url)) {

					if (getPlatformServices() == null) {
						Log.warning(LOG_TAG, "processDestsArray - Platform services are not available");
						return;
					}

					if (getPlatformServices().getNetworkService() == null) {
						Log.debug(LOG_TAG, "processDestsArray - Network services are not available");
						return;
					}

					getPlatformServices().getNetworkService().connectUrlAsync(url, NetworkService.HttpCommand.GET,
							null,
					null, timeout, timeout, new NetworkService.Callback() {
						@Override
						public void call(final NetworkService.HttpConnection connection) {
							if (connection == null) {
								return;
							}

							if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
								Log.trace(LOG_TAG, "processDestsArray - Successfully sent hit.");
							} else {
								Log.trace(LOG_TAG,
										  "processDestsArray - Failed to send hit with connection status (%s).", connection.getResponseCode());
							}

							connection.close();
						}
					});
				}
			}
		} catch (final JsonException ex) {
			Log.debug(LOG_TAG, "processDestsArray - No destinations in response (%s)", ex);
		}
	}

	/**
	 * Loops through the "stuff" array of an AAM response and creates a {@code Map} representing the segments for the user.
	 *
	 * @param jsonResponse the {@link JsonUtilityService.JSONObject} representation of the AAM server response
	 * @return a {@code Map<String, String>} representing the segments for the user
	 */
	private Map<String, String> processStuffArray(final JSONObject jsonResponse) {
		final Map<String, String> returnedMap = new HashMap<String, String>();

		try {
			final JSONArray stuffArray = jsonResponse.getJSONArray(AudienceConstants.AUDIENCE_MANAGER_JSON_STUFF_KEY);

			if (stuffArray == null) {
				// missing the stuff key
				return returnedMap;
			}

			// loop through array and make a more user friendly dictionary
			for (int i = 0; i < stuffArray.length(); i++) {
				final JSONObject stuff = stuffArray.getJSONObject(i);

				if (stuff != null && stuff.length() != 0) {
					final String cookieName = stuff.optString(AudienceConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "");
					final String cookieValue = stuff.optString(AudienceConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "");

					if (!cookieName.isEmpty()) {
						returnedMap.put(cookieName, cookieValue);
					}
				}
			}
		} catch (final JsonException ex) {
			Log.debug(LOG_TAG, "processStuffArray - No 'stuff' array in response (%s)", ex);
		}

		return returnedMap;
	}

	/**
	 * Replaces occurrences of the '.' character with '_' as Audience Manager servers do not allow the '.' character in URL variable names.
	 *
	 * @param name {@link String} containing URL variable name to be sanitized
	 * @return sanitized version of variable name provided
	 */
	private String sanitizeUrlVariableName(final String name) {
		// per aam, they don't allow periods in their url names
		return name.replace(".", "_");
	}

	/**
	 * Creates a shared state object based on the current state for {@code AudienceExtension} using the given version.
	 *
	 * @param currentVersion {@code int} indicating current version of the shared state or eventNumber
	 */
	private void saveAamStateForVersion(final int currentVersion) {
		final AudienceState state = getState();

		if (state == null) {
			Log.debug(LOG_TAG, "saveAamStateForVersion - state is not available");
			return;
		}

		createOrUpdateSharedState(currentVersion, state.getStateData());
	}

	/**
	 * Clear the queue of events waiting to be processed. For each event, if required, dispatch a response event
	 * to any waiting one-time listeners.
	 */
	private void clearWaitingEvents() {
		for (Event e : waitingEvents) {
			dispatchPairedIdResponseIfNecessary(Collections.<String, String>emptyMap(), e);
		}

		waitingEvents.clear();
	}
}
