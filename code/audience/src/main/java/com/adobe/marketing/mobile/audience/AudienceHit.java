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

		event = new Event.Builder("AAM Request", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
		.setResponsePairID(pairId).setTimestamp(timestamp).build();
		event.setEventNumber(eventNumber);

		return event;
	}
}
