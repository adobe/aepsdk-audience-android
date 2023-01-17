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

/**
 * AudienceHit class represents the in-memory object interface to database records that hold queued Audience Manager network requests.
 */
class AudienceHit extends AbstractHit {

	/**
	 * Indicates the number of seconds a network request should wait for server response.
	 */
	int timeout;

	/**
	 * Contains optional event pairing identifier, used with one-time listeners.
	 */
	String pairId;

	/**
	 * Contains the URL for the Audience Manager request.
	 */
	String url;

	/**
	 * Contains the event number assigned by {@code EventHub}, used to retrieve module shared states.
	 */
	int eventNumber;

	/**
	 * Contains the internal {@code Event} representation of this {@code AudienceHit} instance.
	 */
	private Event event;

	/**
	 * Returns this {@link #event} created using the internal properties of the instance.
	 *
	 * @return {@code Event} instance
	 */
	Event getEvent() {
		if (event != null) {
			return event;
		}

		event =
			new Event.Builder("AAM Request", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
				.setResponsePairID(pairId)
				.setTimestamp(timestamp)
				.build();
		event.setEventNumber(eventNumber);

		return event;
	}
}
