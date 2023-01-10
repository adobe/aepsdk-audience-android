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
 * Dispatches {@code AUDIENCEMANAGER}, {@code RESPONSE_IDENTITY} events onto the {@code EventHub}.
 */
class DispatcherAudienceResponseIdentityAudienceManager extends ModuleEventDispatcher<AudienceExtension> {
	/**
	 * Constructor
	 *
	 * @param eventHub (required) an {@link EventHub} instance used by this dispatcher
	 * @param extension parent {@link AudienceExtension} that owns this dispatcher
	 */
	DispatcherAudienceResponseIdentityAudienceManager(final EventHub eventHub, final AudienceExtension extension) {
		super(eventHub, extension);
	}

	/**
	 * Dispatches {@code AUDIENCEMANAGER}, {@code RESPONSE_IDENTITY} event onto the {@code EventHub} containing
	 * visitor {@code profile}, {@code dpid} and {@code dpuuid} for the given {@code pairId}.
	 *
	 * @param profile current Audience Manager Visitor Profile {@link Map}
	 * @param dpid current Data Provider ID
	 * @param dpuuid current Data Provider User ID
	 * @param pairId A unique pairing id for one-time listeners
	 */
	void dispatch(final Map<String, String> profile, final String dpid, final String dpuuid, final String pairId) {
		// create the event data from the profile, dpid and dpuuid
		final EventData eventData = new EventData();
		eventData.putStringMap(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE, profile);
		eventData.putString(AudienceConstants.EventDataKeys.Audience.DPID, dpid);
		eventData.putString(AudienceConstants.EventDataKeys.Audience.DPUUID, dpuuid);
		// create the event
		final Event event = new Event.Builder("Audience Manager Identities", EventType.AUDIENCEMANAGER,
											  EventSource.RESPONSE_IDENTITY).setData(eventData).setPairID(pairId).build();
		// dispatch
		dispatch(event);
	}
}
