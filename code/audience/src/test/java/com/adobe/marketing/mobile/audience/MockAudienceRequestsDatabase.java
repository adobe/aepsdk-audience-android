/*
  Copyright 2017 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

public class MockAudienceRequestsDatabase extends AudienceHitsDatabase {

	public MockAudienceRequestsDatabase(final AudienceExtension parent, final PlatformServices services) {
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
