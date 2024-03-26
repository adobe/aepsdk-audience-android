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

package com.adobe.audiencetestapp;

import static com.adobe.audiencetestapp.AudienceTestApp.LOG_TAG;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.adobe.marketing.mobile.Analytics;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Audience;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Deep links handling
        final Intent intent = getIntent();
        final Uri data = intent.getData();

        if (data != null) {
            Assurance.startSession(data.toString());
            Log.d(LOG_TAG, "Deep link received " + data);
        }

        enableLifecycle();
        setExtensionVersionsForDebug();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        if (item.getItemId() == R.id.connectToAssurance) {
            intent = new Intent(MainActivity.this, AssuranceActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setAdID(View view) {
        MobileCore.setAdvertisingIdentifier("advertising-id-123");
    }

    public void trackState(View view) {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "value");
        MobileCore.trackState("state", data);
    }

    public void trackAction(View view) {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "value");
        MobileCore.trackState("action", data);
    }

    public void getSDKIdentities(View view) {
        MobileCore.getSdkIdentities(
                s -> {
                    Log.d(LOG_TAG, "SDKIdentities received: " + s);
                    updateTvData(view, s);
                });
    }

    public void submitSignal(View view) {
        HashMap<String, String> map = new HashMap<>();
        map.put("mykey", "myvalue");
        Audience.signalWithData(map, value -> Log.d(LOG_TAG, "stuff"));
    }

    public void getVisitorProfile(View view) {
        Audience.getVisitorProfile(
                value -> {
                    final String visitorProfile = value != null ? value.toString() : "null";
                    Log.d(LOG_TAG, "Visitor profile received: " + visitorProfile);
                    updateTvData(view, visitorProfile);
                });
    }

    public void reset(View view) {
        Audience.reset();
        sendToast("Reset API called");
    }

    public void setPrivacyOptIn(View view) {
        MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
    }

    public void setPrivacyOptOut(View view) {
        MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
    }

    public void setPrivacyUnknown(View view) {
        MobileCore.setPrivacyStatus(MobilePrivacyStatus.UNKNOWN);
    }

    public void sendToast(final String msg) {
        new Handler(Looper.getMainLooper())
                .post(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private void enableLifecycle() {
        getApplication()
                .registerActivityLifecycleCallbacks(
                        new Application.ActivityLifecycleCallbacks() {
                            @Override
                            public void onActivityResumed(Activity activity) {
                                MobileCore.setApplication(getApplication());
                                MobileCore.lifecycleStart(null);
                            }

                            @Override
                            public void onActivityPaused(Activity activity) {
                                MobileCore.lifecyclePause();
                            }

                            // the following methods aren't needed for our lifecycle purposes, but
                            // are
                            // required to be implemented by the ActivityLifecycleCallbacks object
                            @Override
                            public void onActivityCreated(
                                    Activity activity, Bundle savedInstanceState) {}

                            @Override
                            public void onActivityStarted(Activity activity) {}

                            @Override
                            public void onActivityStopped(Activity activity) {}

                            @Override
                            public void onActivitySaveInstanceState(
                                    Activity activity, Bundle outState) {}

                            @Override
                            public void onActivityDestroyed(Activity activity) {}
                        });
    }

    private void updateTvData(final View view, final String text) {
        final TextView tvVisitorProfile = findViewById(R.id.tvResultData);
        view.post(
                () -> {
                    if (tvVisitorProfile != null) {
                        tvVisitorProfile.setText(text);
                    }
                });
    }

    private void setExtensionVersionsForDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append("Running with: ");
        sb.append("Audience v").append(Audience.extensionVersion());
        sb.append(" | Core v").append(MobileCore.extensionVersion());
        sb.append(" | Identity v").append(Identity.extensionVersion());
        sb.append(" | Lifecycle v").append(Lifecycle.extensionVersion());
        sb.append(" | Analytics v").append(Analytics.extensionVersion());
        sb.append(" | Assurance v").append(Assurance.extensionVersion());
        final TextView tvExtensions = findViewById(R.id.tvExtensions);

        if (tvExtensions != null) {
            tvExtensions.setText(sb.toString());
        }
    }
}
