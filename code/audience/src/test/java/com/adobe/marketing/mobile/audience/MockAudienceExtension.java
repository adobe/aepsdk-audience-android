/*
  Copyright 2020 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import java.util.HashMap;

public class MockAudienceExtension extends AudienceExtension {

	MockAudienceExtension(EventHub eventHub, PlatformServices platformServices) {
		super(eventHub, platformServices);
	}

	boolean submitSignalWasCalled;
	int submitSignalCallCount = 0;
	Event submitSignalParameterEvent;

	@Override
	protected void submitSignal(final Event event) {
		submitSignalWasCalled = true;
		submitSignalParameterEvent = event;
		submitSignalCallCount++;
	}

	boolean getIdentityVariablesWasCalled;
	String getIdentityVariablesParameterPairId;

	@Override
	void getIdentityVariables(final String pairId) {
		getIdentityVariablesWasCalled = true;
		getIdentityVariablesParameterPairId = pairId;
	}

	boolean setDpidAndDpuuidWasCalled;
	String setDpidAndDpuuidParameterDpid;
	String setDpidAndDpuuidParameterDpuuid;
	Event setDpidAndDpuuidParameterEvent;

	@Override
	void setDpidAndDpuuid(final String dpid, final String dpuuid, final Event event) {
		setDpidAndDpuuidParameterEvent = event;
		setDpidAndDpuuidWasCalled = true;
		setDpidAndDpuuidParameterDpid = dpid;
		setDpidAndDpuuidParameterDpuuid = dpuuid;
	}

	boolean resetWasCalled;
	Event resetParameterEvent;

	@Override
	void reset(final Event event) {
		resetParameterEvent = event;
		resetWasCalled = true;
	}

	boolean bootupWasCalled;
	Event bootEvent;

	@Override
	void bootup(final Event event) {
		bootEvent = event;
		bootupWasCalled = true;
	}

	boolean processResponseWasCalled;
	String processResponseParameterResponse;
	Event processResponseParameterEvent;

	@Override
	protected HashMap<String, String> processResponse(final String response, final Event event) {
		processResponseWasCalled = true;
		processResponseParameterResponse = response;
		processResponseParameterEvent = event;
		return null;
	}

	boolean processStateChangeWasCalled = false;
	String processStateChangeParameterStateName;

	@Override
	protected void processStateChange(final String stateName) {
		processStateChangeParameterStateName = stateName;
		processStateChangeWasCalled = true;
	}

	boolean handleNetworkResponseWasCalled = false;
	String handleNetworkResponseParamResponse;
	Event handleNetworkResponseParamEvent;

	@Override
	void handleNetworkResponse(String response, Event event) {
		handleNetworkResponseWasCalled = true;
		handleNetworkResponseParamResponse = response;
		handleNetworkResponseParamEvent = event;
	}

	boolean processConfigurationWasCalled = false;
	Event processConfigurationParameterEvent;

	@Override
	void processConfiguration(Event event) {
		processConfigurationWasCalled = true;
		processConfigurationParameterEvent = event;
		super.processConfiguration(event);
	}
}
