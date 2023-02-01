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

public class AudienceTestConstants {

	public static final String LOG_TAG = "AudienceTests";

	public static final String UUID_KEY = "uuid";
	public static final String VISITOR_PROFILE_KEY = "aamprofile";
	public static final String RESPONSE_PROFILE_DATA = "profileDataKey";

	public static class EventType {

		static final String MONITOR = "com.adobe.functional.eventType.monitor";

		private EventType() {}
	}

	public static class EventSource {

		// Used by Monitor Extension
		static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
		static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
		static final String UNREGISTER = "com.adobe.eventSource.unregister";

		private EventSource() {}
	}

	public static class EventDataKey {

		static final String STATE_OWNER = "stateowner";

		private EventDataKey() {}
	}

	static final class DataStoreKey {

		public static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
		public static final String AUDIENCE_DATASTORE = "AAMDataStore";

		private DataStoreKey() {}
	}
}
