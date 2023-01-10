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
	String  processResponseParameterResponse;
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
