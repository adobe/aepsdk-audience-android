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

package com.adobe.marketing.mobile.audience.testapp;

import android.app.Application;
import android.util.Log;
import com.adobe.marketing.mobile.Analytics;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Audience;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import java.util.Arrays;
import java.util.HashMap;

public class AudienceTestApp extends Application {

    public static final String LOG_TAG = "AudienceTestApp";
    // TODO: Set up the Environment File ID from your mobile property for the preferred environment
    // configured in Data Collection UI
    private final String ENVIRONMENT_FILE_ID = "";

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCore.setApplication(this);
        MobileCore.setLogLevel(LoggingMode.VERBOSE);
        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

        MobileCore.registerExtensions(
                Arrays.asList(
                        Identity.EXTENSION,
                        Audience.EXTENSION,
                        Lifecycle.EXTENSION,
                        Analytics.EXTENSION,
                        Assurance.EXTENSION),
                o -> {
                    Log.d(LOG_TAG, "Mobile SDK was initialized");
                    // testWithAAMForwardingForAnalytics(false);
                });
    }

    private void testWithAAMForwardingForAnalytics(final boolean aamForwardingEnabled) {
        MobileCore.updateConfiguration(
                new HashMap<String, Object>() {
                    {
                        put("analytics.aamForwardingEnabled", aamForwardingEnabled);
                    }
                });
    }
}
