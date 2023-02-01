/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceTestConstants.LOG_TAG;

import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A monitoring extension class aiding for assertion against dispatched events, shared state
 * and XDM shared state.
 */
public class MonitorExtension extends Extension {

	public static final Class<? extends Extension> EXTENSION = MonitorExtension.class;

	private static final String LOG_SOURCE = "MonitorExtension";

	private static final Map<EventSpec, List<Event>> receivedEvents = new HashMap<>();

	protected MonitorExtension(ExtensionApi extensionApi) {
		super(extensionApi);
	}

	@NonNull
	@Override
	protected String getName() {
		return "MonitorExtension";
	}

	@Override
	protected void onRegistered() {
		super.onRegistered();
		getApi().registerEventListener(EventType.WILDCARD, EventSource.WILDCARD, this::wildcardProcessor);
	}

	/**
	 * Unregister the Monitor Extension from the EventHub.
	 */
	public static void unregisterExtension() {
		Event event = new Event.Builder(
			"Unregister Monitor Extension Request",
			AudienceTestConstants.EventType.MONITOR,
			AudienceTestConstants.EventSource.UNREGISTER
		)
			.build();
		MobileCore.dispatchEvent(event);
	}

	public static Map<EventSpec, List<Event>> getReceivedEvents() {
		return receivedEvents;
	}

	/**
	 * Resets the map of received and expected events.
	 */
	public static void reset() {
		Log.trace(LOG_TAG, LOG_SOURCE, "Reset expected and received events.");
		receivedEvents.clear();
	}

	/**
	 * Processor for all heard events.
	 * If the event type is of this Monitor Extension, then
	 * the action is performed per the event source.
	 * All other events are added to the map of received events. If the event is in the map
	 * of expected events, its latch is counted down.
	 *
	 * @param event
	 */
	public void wildcardProcessor(final Event event) {
		if (AudienceTestConstants.EventType.MONITOR.equalsIgnoreCase(event.getType())) {
			if (AudienceTestConstants.EventSource.SHARED_STATE_REQUEST.equalsIgnoreCase(event.getSource())) {
				processSharedStateRequest(event);
			} else if (AudienceTestConstants.EventSource.UNREGISTER.equalsIgnoreCase(event.getSource())) {
				processUnregisterRequest(event);
			}

			return;
		}

		EventSpec eventSpec = new EventSpec(event.getSource(), event.getType());

		Log.debug(LOG_TAG, LOG_SOURCE, "Received and processing event" + eventSpec);

		if (!receivedEvents.containsKey(eventSpec)) {
			receivedEvents.put(eventSpec, new ArrayList<>());
		}

		receivedEvents.get(eventSpec).add(event);
	}

	/**
	 * Processor which unregisters this extension.
	 * @param event
	 */
	private void processUnregisterRequest(final Event event) {
		Log.debug(LOG_TAG, LOG_SOURCE, "Unregistering the Monitor Extension.");
		getApi().unregisterExtension();
	}

	/**
	 * Processor which retrieves and dispatches the shared state for the state owner specified
	 * in the request.
	 * @param event
	 */
	private void processSharedStateRequest(final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null) {
			return;
		}

		final String stateOwner = DataReader.optString(eventData, AudienceTestConstants.EventDataKey.STATE_OWNER, null);

		if (stateOwner == null) {
			return;
		}

		final SharedStateResult sharedStateResult = getApi()
			.getSharedState(stateOwner, event, false, SharedStateResolution.LAST_SET);

		Event responseEvent = new Event.Builder(
			"Get Shared State Response",
			AudienceTestConstants.EventType.MONITOR,
			AudienceTestConstants.EventSource.SHARED_STATE_RESPONSE
		)
			.setEventData(sharedStateResult == null ? null : sharedStateResult.getValue())
			.inResponseToEvent(event)
			.build();

		MobileCore.dispatchEvent(responseEvent);
	}

	/**
	 * Class defining {@link Event} specifications, contains Event's source and type.
	 */
	public static class EventSpec {

		final String source;
		final String type;

		public EventSpec(final String source, final String type) {
			if (source == null || source.isEmpty()) {
				throw new IllegalArgumentException("Event Source cannot be null or empty.");
			}

			if (type == null || type.isEmpty()) {
				throw new IllegalArgumentException("Event Type cannot be null or empty.");
			}
			// Normalize strings
			this.source = source.toLowerCase();
			this.type = type.toLowerCase();
		}

		@Override
		public String toString() {
			return "type '" + type + "' and source '" + source + "'";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			EventSpec eventSpec = (EventSpec) o;
			return Objects.equals(source, eventSpec.source) && Objects.equals(type, eventSpec.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, type);
		}
	}
}
