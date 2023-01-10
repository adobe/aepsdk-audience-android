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

import java.util.Map;

/**
 * Dispatches {@code AUDIENCEMANAGER}, {@code RESPONSE_CONTENT} events onto the {@code EventHub}.
 */
class DispatcherAudienceResponseContentAudienceManager extends ModuleEventDispatcher<AudienceExtension> {
	/**
	 * Constructor
	 *
	 * @param eventHub (required) an {@link EventHub} instance used by this dispatcher
	 * @param module parent {@link AudienceExtension} module that owns this dispatcher
	 */
	DispatcherAudienceResponseContentAudienceManager(final EventHub eventHub, final AudienceExtension module) {
		super(eventHub, module);
	}

	/**
	 * Dispatches {@code AUDIENCEMANAGER}, {@code RESPONSE_CONTENT} event onto the {@code EventHub} for the given {@code profileMap}
	 * and {@code pairId}.
	 *
	 * @param profileMap {@code Map<String, String>} containing AAM segments associated with the current user
	 * @param pairId A unique pairing id for one-time listeners
	 */
	void dispatch(final Map<String, String> profileMap, final String pairId) {
		// create the event data from the profile map
		final EventData eventData = new EventData();
		eventData.putStringMap(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE, profileMap);
		// create the event
		final Event event = new Event.Builder("Audience Manager Profile", EventType.AUDIENCEMANAGER,
											  EventSource.RESPONSE_CONTENT).setData(eventData).setPairID(pairId).build();
		// dispatch
		dispatch(event);
	}

	/**
	 * Dispatches {@code AUDIENCEMANAGER}, {@code RESPONSE_CONTENT} event onto the {@code EventHub} containing the result of the
	 * opt-out send.
	 *
	 * @param optedOut The result that needs to be communicated
	 *
	 * @see  AudienceExtension#sendOptOutHit(EventData)
	 */
	void dispatchOptOutResult(final boolean optedOut) {
		// create the event data from the profile map
		final EventData eventData = new EventData();
		eventData.putBoolean(AudienceConstants.EventDataKeys.Audience.OPTED_OUT_HIT_SENT, optedOut);
		// create the event
		final Event event = new Event.Builder("Audience Manager Opt Out Event", EventType.AUDIENCEMANAGER,
											  EventSource.RESPONSE_CONTENT).setData(eventData).build();
		// dispatch
		dispatch(event);
	}
}
