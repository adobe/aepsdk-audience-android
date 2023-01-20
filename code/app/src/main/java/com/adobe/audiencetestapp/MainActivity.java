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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Audience;
import com.adobe.marketing.mobile.MobileCore;
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
		MobileCore.getSdkIdentities(s -> System.out.println("#SDKIdentities - " + s));
	}

	public void submitSignal(View view) {
		HashMap<String, String> map = new HashMap<>();
		map.put("mykey", "myvalue");
		Audience.signalWithData(map, value -> Log.d(LOG_TAG, "stuff"));
	}

	public void getVisitorProfile(View view) {
		Audience.getVisitorProfile(value ->
			Log.d(LOG_TAG, "Visitor profile received: " + (value != null ? value.toString() : "null"))
		);
	}

	public void reset(View view) {
		Audience.reset();
		sendToast("Reset API called");
	}

	public void sendToast(final String msg) {
		new Handler(Looper.getMainLooper())
			.post(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
	}
}
