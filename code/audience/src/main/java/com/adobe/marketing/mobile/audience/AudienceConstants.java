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

import com.adobe.marketing.mobile.MobilePrivacyStatus;
import java.util.HashMap;

/**
 * This class holds all constant values used only by the Audience module
 */
class AudienceConstants {

	// general strings
	static final String EXTENSION_NAME = "com.adobe.module.audience";
	static final String FRIENDLY_EXTENSION_NAME = "Audience";
	static final String LOG_TAG = FRIENDLY_EXTENSION_NAME;

	// destination variable keys
	static final String AUDIENCE_MANAGER_DATA_PROVIDER_ID_KEY = "d_dpid";
	static final String AUDIENCE_MANAGER_DATA_PROVIDER_USER_ID_KEY = "d_dpuuid";
	static final String AUDIENCE_MANAGER_USER_ID_KEY = "d_uuid";
	static final String MARKETING_CLOUD_ORG_ID = "d_orgid";
	static final String VISITOR_ID_MID_KEY = "d_mid";
	static final String VISITOR_ID_BLOB_KEY = "d_blob";
	static final String VISITOR_ID_LOCATION_HINT_KEY = "dcs_region";
	static final String VISITOR_ID_PARAMETER_KEY_CUSTOMER = "d_cid_ic";
	static final String VISITOR_ID_CID_DELIMITER = "%01";

	// url stitching
	static final String AUDIENCE_MANAGER_EVENT_PATH = "event";
	static final String AUDIENCE_MANAGER_CUSTOMER_DATA_PREFIX = "c_";
	static final String AUDIENCE_MANAGER_URL_PARAM_DST = "d_dst=1";
	static final String AUDIENCE_MANAGER_URL_PARAM_RTBD = "d_rtbd=json";
	static final String AUDIENCE_MANAGER_URL_PLATFORM_KEY = "d_ptfm=";

	// persistent storage
	static final String AUDIENCE_MANAGER_SHARED_PREFS_DATA_STORE = "AAMDataStore";
	static final String AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY = "AAMUserProfile";
	static final String AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY = "AAMUserId";

	// json response keys
	static final String AUDIENCE_MANAGER_JSON_DESTS_KEY = "dests";
	static final String AUDIENCE_MANAGER_JSON_URL_KEY = "c";
	static final String AUDIENCE_MANAGER_JSON_STUFF_KEY = "stuff";
	static final String AUDIENCE_MANAGER_JSON_USER_ID_KEY = "uuid";
	static final String AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY = "cn";
	static final String AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY = "cv";

	// config defaults
	static final int DEFAULT_AAM_TIMEOUT = 2;
	static final MobilePrivacyStatus DEFAULT_PRIVACY_STATUS = MobilePrivacyStatus.UNKNOWN;

	//opt-out end-points
	static final String AUDIENCE_MANAGER_OPT_OUT_URL_BASE = "https://%s/demoptout.jpg?";
	static final String AUDIENCE_MANAGER_OPT_OUT_URL_AAM = "d_uuid=%s";

	// databases
	static final String DEPRECATED_1X_HIT_DATABASE_FILENAME = "ADBMobileAAM.sqlite";

	static final HashMap<String, String> MAP_TO_CONTEXT_DATA_KEYS = createMap();

	static HashMap<String, String> createMap() {
		final HashMap<String, String> map = new HashMap<String, String>();
		map.put(EventDataKeys.Identity.ADVERTISING_IDENTIFIER, ContextDataKeys.ADVERTISING_IDENTIFIER);
		map.put(EventDataKeys.Lifecycle.APP_ID, ContextDataKeys.APPLICATION_IDENTIFIER);
		map.put(EventDataKeys.Lifecycle.CARRIER_NAME, ContextDataKeys.CARRIER_NAME);
		map.put(EventDataKeys.Lifecycle.CRASH_EVENT, ContextDataKeys.CRASH_EVENT_KEY);
		map.put(EventDataKeys.Lifecycle.DAILY_ENGAGED_EVENT, ContextDataKeys.DAILY_ENGAGED_EVENT_KEY);
		map.put(EventDataKeys.Lifecycle.DAY_OF_WEEK, ContextDataKeys.DAY_OF_WEEK);
		map.put(EventDataKeys.Lifecycle.DAYS_SINCE_FIRST_LAUNCH, ContextDataKeys.DAYS_SINCE_FIRST_LAUNCH);
		map.put(EventDataKeys.Lifecycle.DAYS_SINCE_LAST_LAUNCH, ContextDataKeys.DAYS_SINCE_LAST_LAUNCH);
		map.put(EventDataKeys.Lifecycle.DAYS_SINCE_LAST_UPGRADE, ContextDataKeys.DAYS_SINCE_LAST_UPGRADE);
		map.put(EventDataKeys.Lifecycle.DEVICE_NAME, ContextDataKeys.DEVICE_NAME);
		map.put(EventDataKeys.Lifecycle.DEVICE_RESOLUTION, ContextDataKeys.DEVICE_RESOLUTION);
		map.put(EventDataKeys.Lifecycle.HOUR_OF_DAY, ContextDataKeys.HOUR_OF_DAY);
		map.put(EventDataKeys.Lifecycle.IGNORED_SESSION_LENGTH, ContextDataKeys.IGNORED_SESSION_LENGTH);
		map.put(EventDataKeys.Lifecycle.INSTALL_DATE, ContextDataKeys.INSTALL_DATE);
		map.put(EventDataKeys.Lifecycle.INSTALL_EVENT, ContextDataKeys.INSTALL_EVENT_KEY);
		map.put(EventDataKeys.Lifecycle.LAUNCH_EVENT, ContextDataKeys.LAUNCH_EVENT_KEY);
		map.put(EventDataKeys.Lifecycle.LAUNCHES, ContextDataKeys.LAUNCHES);
		map.put(EventDataKeys.Lifecycle.LAUNCHES_SINCE_UPGRADE, ContextDataKeys.LAUNCHES_SINCE_UPGRADE);
		map.put(EventDataKeys.Lifecycle.LOCALE, ContextDataKeys.LOCALE);
		map.put(EventDataKeys.Lifecycle.MONTHLY_ENGAGED_EVENT, ContextDataKeys.MONTHLY_ENGAGED_EVENT_KEY);
		map.put(EventDataKeys.Lifecycle.OPERATING_SYSTEM, ContextDataKeys.OPERATING_SYSTEM);
		map.put(EventDataKeys.Lifecycle.PREVIOUS_SESSION_LENGTH, ContextDataKeys.PREVIOUS_SESSION_LENGTH);
		map.put(EventDataKeys.Lifecycle.RUN_MODE, ContextDataKeys.RUN_MODE);
		map.put(EventDataKeys.Lifecycle.UPGRADE_EVENT, ContextDataKeys.UPGRADE_EVENT_KEY);

		return map;
	}

	// constructor for code coverage reports to be satiated
	AudienceConstants() {}

	/**
	 * Analytics context data keys
	 */
	static final class ContextDataKeys {

		static final String INSTALL_EVENT_KEY = "a.InstallEvent";
		static final String LAUNCH_EVENT_KEY = "a.LaunchEvent";
		static final String CRASH_EVENT_KEY = "a.CrashEvent";
		static final String UPGRADE_EVENT_KEY = "a.UpgradeEvent";
		static final String DAILY_ENGAGED_EVENT_KEY = "a.DailyEngUserEvent";
		static final String MONTHLY_ENGAGED_EVENT_KEY = "a.MonthlyEngUserEvent";
		static final String INSTALL_DATE = "a.InstallDate";
		static final String LAUNCHES = "a.Launches";
		static final String PREVIOUS_SESSION_LENGTH = "a.PrevSessionLength";
		static final String DAYS_SINCE_FIRST_LAUNCH = "a.DaysSinceFirstUse";
		static final String DAYS_SINCE_LAST_LAUNCH = "a.DaysSinceLastUse";
		static final String HOUR_OF_DAY = "a.HourOfDay";
		static final String DAY_OF_WEEK = "a.DayOfWeek";
		static final String OPERATING_SYSTEM = "a.OSVersion";
		static final String APPLICATION_IDENTIFIER = "a.AppID";
		static final String DAYS_SINCE_LAST_UPGRADE = "a.DaysSinceLastUpgrade";
		static final String LAUNCHES_SINCE_UPGRADE = "a.LaunchesSinceUpgrade";
		static final String ADVERTISING_IDENTIFIER = "a.adid";
		static final String DEVICE_NAME = "a.DeviceName";
		static final String DEVICE_RESOLUTION = "a.Resolution";
		static final String CARRIER_NAME = "a.CarrierName";
		static final String LOCALE = "a.locale";
		static final String RUN_MODE = "a.RunMode";
		static final String IGNORED_SESSION_LENGTH = "a.ignoredSessionLength";

		private ContextDataKeys() {}
	}

	/*
		EventDataKeys
	 */
	static final class EventDataKeys {

		static final String STATE_OWNER = "stateowner";

		private EventDataKeys() {}

		static final class Analytics {

			static final String ANALYTICS_SERVER_RESPONSE_KEY = "analyticsserverresponse";

			private Analytics() {}
		}

		static final class Audience {

			static final String MODULE_NAME = "com.adobe.module.audience";

			// request keys
			static final String VISITOR_TRAITS = "aamtraits";

			// response keys
			static final String VISITOR_PROFILE = "aamprofile";
			static final String AUDIENCE_IDS = "audienceids";
			static final String DPID = "dpid";
			static final String DPUUID = "dpuuid";
			static final String UUID = "uuid";
			//opted out response key
			static final String OPTED_OUT_HIT_SENT = "optedouthitsent";

			private Audience() {}
		}

		static final class Configuration {

			static final String MODULE_NAME = "com.adobe.module.configuration";

			// config response keys
			static final String GLOBAL_CONFIG_PRIVACY = "global.privacy";
			static final String AAM_CONFIG_SERVER = "audience.server";
			static final String AAM_CONFIG_TIMEOUT = "audience.timeout";
			static final String EXPERIENCE_CLOUD_ORGID = "experienceCloud.org";
			static final String ANALYTICS_CONFIG_AAMFORWARDING = "analytics.aamForwardingEnabled";

			private Configuration() {}
		}

		static final class Identity {

			static final String MODULE_NAME = "com.adobe.module.identity";
			static final String VISITOR_ID_MID = "mid";
			static final String VISITOR_ID_BLOB = "blob";
			static final String VISITOR_ID_LOCATION_HINT = "locationhint";
			static final String VISITOR_IDS_LIST = "visitoridslist";
			static final String ADVERTISING_IDENTIFIER = "advertisingidentifier";

			private Identity() {}
		}

		static final class Lifecycle {

			static final String APP_ID = "appid";
			static final String CARRIER_NAME = "carriername";
			static final String CRASH_EVENT = "crashevent";
			static final String DAILY_ENGAGED_EVENT = "dailyenguserevent";
			static final String DAY_OF_WEEK = "dayofweek";
			static final String DAYS_SINCE_FIRST_LAUNCH = "dayssincefirstuse";
			static final String DAYS_SINCE_LAST_LAUNCH = "dayssincelastuse";
			static final String DAYS_SINCE_LAST_UPGRADE = "dayssincelastupgrade";
			static final String DEVICE_NAME = "devicename";
			static final String DEVICE_RESOLUTION = "resolution";
			static final String HOUR_OF_DAY = "hourofday";
			static final String IGNORED_SESSION_LENGTH = "ignoredsessionlength";
			static final String INSTALL_DATE = "installdate";
			static final String INSTALL_EVENT = "installevent";
			static final String LAUNCH_EVENT = "launchevent";
			static final String LAUNCHES = "launches";
			static final String LAUNCHES_SINCE_UPGRADE = "launchessinceupgrade";
			static final String LIFECYCLE_CONTEXT_DATA = "lifecyclecontextdata";
			static final String LOCALE = "locale";
			static final String MONTHLY_ENGAGED_EVENT = "monthlyenguserevent";
			static final String OPERATING_SYSTEM = "osversion";
			static final String PREVIOUS_SESSION_LENGTH = "prevsessionlength";
			static final String RUN_MODE = "runmode";
			static final String UPGRADE_EVENT = "upgradeevent";

			private Lifecycle() {}
		}
	}
}
