/***************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2017 Adobe Systems Incorporated
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
 *
 **************************************************************************/
package com.adobe.marketing.mobile;

public class TestableAudience extends AudienceExtension {

	public TestableAudience(final EventHub hub, final PlatformServices services,
							final DispatcherAudienceResponseContentAudienceManager dispatcherAudienceResponseContent,
							final DispatcherAudienceResponseIdentityAudienceManager dispatcherAudienceResponseIdentity,
							final AudienceState state) throws MissingPlatformServicesException {
		super(hub, services);
		super.dispatcherAudienceResponseContent = dispatcherAudienceResponseContent;
		super.dispatcherAudienceResponseIdentity = dispatcherAudienceResponseIdentity;
		super.internalState = state;
		super.internalDatabase = new MockAudienceRequestsDatabase(this, services);
	}

	boolean processQueuedEventsCalled = false;
	@Override
	protected void processQueuedEvents() {
		super.processQueuedEvents();
		processQueuedEventsCalled = true;
	}

	boolean resetWasCalled = false;
	Event resetParameterEvent;
	@Override
	void reset(Event event) {
		resetWasCalled = true;
		resetParameterEvent = event;
		super.reset(event);
	}

	EventData getSharedAAMStateFromEventHub(final Event event) {
		return getSharedEventState("com.adobe.module.audience", event);
	}
}
