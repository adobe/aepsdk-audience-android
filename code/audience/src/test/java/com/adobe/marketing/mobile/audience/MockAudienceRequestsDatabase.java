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

public class MockAudienceRequestsDatabase extends AudienceHitsDatabase {
	public MockAudienceRequestsDatabase(final AudienceExtension parent,	final PlatformServices services) {
		super(parent, services);
	}

	boolean queueWasCalled = false;
	String queueParameterUrl;
	int queueParameterTimeout;
	Event queueParameterEvent;

	@Override
	void queue(final String url, final int timeout, final MobilePrivacyStatus privacyStatus, final Event event) {
		queueWasCalled = true;
		queueParameterUrl = url;
		queueParameterTimeout = timeout;
		queueParameterEvent = event;
	}


	boolean updatePrivacyStatusWasCalled = false;
	MobilePrivacyStatus updatePrivacyStatusParameterPrivacyStatus;
	@Override
	void updatePrivacyStatus(MobilePrivacyStatus privacyStatus) {
		updatePrivacyStatusWasCalled = true;
		updatePrivacyStatusParameterPrivacyStatus = privacyStatus;
	}
}
