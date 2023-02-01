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

package com.adobe.marketing.mobile.audience;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AudienceModuleTest {

	private final Map<String, Object> config = new HashMap<String, Object>() {
		{
			put("audience.server", "server");
			put("audience.timeout", 5);
			put("global.privacy", "optedin");
			put("analytics.aamForwardingEnabled", false);
			put("experienceCloud.org", "test@AdobeOrg");
		}
	};

	private TestableNetworkService testableNetworkService;

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	@Before
	public void beforeEach() throws InterruptedException {
		testableNetworkService = new TestableNetworkService();
		ServiceProvider.getInstance().setNetworkService(testableNetworkService);
		TestHelper.registerExtensions(
			Arrays.asList(MonitorExtension.EXTENSION, Audience.EXTENSION, Identity.EXTENSION),
			config
		);
	}

	//	@Test
	//	public void testGet_VisitorProfile_when_VisitorProfileInPersistence() throws Exception {
	//		// Setup Variables
	//		final HashMap<String, Object> audienceResponse = new HashMap<String, Object>();
	//		final CountDownLatch latch = new CountDownLatch(1);
	//
	//		// Setup expectations
	//		eventHub.setExpectedEventCount(5);
	//		mockVisitorProfileInPersistence();
	//
	//		// Create callback for getter event
	//		AdobeCallback<EventData> callback = new AdobeCallback<EventData>() {
	//			@Override
	//			public void call(EventData eventData) {
	//				audienceResponse.put(DPID_KEY, eventData.optString("dpid", null));
	//				audienceResponse.put(DPUUID_KEY, eventData.optString("dpuuid", null));
	//				audienceResponse.put(VISITOR_PROFILE_KEY, eventData.optStringMap("aamprofile", null));
	//				latch.countDown();
	//			}
	//		};
	//
	//		// Dispatch getter event
	//		eventHub.dispatch(createAudienceRequestIdentityEvent(callback));
	//		latch.await(1, TimeUnit.SECONDS);
	//		List<Event> events = eventHub.getEvents();
	//
	//		// Check if 2 events are dispatched
	//		assertEquals(2, events.size());
	//
	//		// check if the getter request event is dispatched
	//		Event event1 = events.get(0);
	//		assertEquals(EventType.AUDIENCEMANAGER, event1.getType());
	//		assertEquals(EventSource.REQUEST_IDENTITY, event1.getSource());
	//
	//		// check if the getter's response event is dispatched
	//		Event event2 = events.get(1);
	//		assertEquals(EventType.AUDIENCEMANAGER, event2.getType());
	//		assertEquals(EventSource.RESPONSE_IDENTITY, event2.getSource());
	//		assertNull(event2.getData().optString(DPUUID_KEY, null));
	//		assertNull(event2.getData().optString(DPID_KEY, null));
	//		//assertEquals("testDpid", event2.getData().getString("aamProfile"));
	//
	//		// check if the callback is called with correct data
	//		assertNull(audienceResponse.get(DPID_KEY));
	//		assertNull(audienceResponse.get(DPUUID_KEY));
	//		HashMap<String, String> aamProfile = (HashMap<String, String>) audienceResponse.get("aamprofile");
	//		assertEquals("visitorValue", aamProfile.get("visitorKey"));
	//	}

	//	// =============================================================================
	//	// Test Submit Signal (checks on network request)
	//	// =============================================================================
	//
	//	// =============================================================================
	//	// Test Submit Signal (mock network request and test further)
	//	// =============================================================================
	//
	//	@Test
	//	public void testSubmitSignal_when_NetworkHasUnRecoverableError_then_callBackCalledWithEmptyProfile()
	//		throws Exception {
	//		eventHub.setExpectedEventCount(3);
	//		eventHub.ignoreEvents(EventType.HUB, EventSource.SHARED_STATE);
	//		final HashMap<String, Object> audienceResponse = new HashMap<String, Object>();
	//		final CountDownLatch latch = new CountDownLatch(1);
	//		testableNetworkService.setExpectedCount(2);
	//		TestableNetworkService.NetworkResponse response = new TestableNetworkService.NetworkResponse(
	//			(String) null,
	//			404,
	//			null
	//		);
	//		testableNetworkService.setDefaultResponse(response);
	//
	//		// Preset the shared state and shared Preferences
	//		providedValidConfigurationState();
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//
	//		// Test
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		AdobeCallback<HashMap<String, String>> callback = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback));
	//		latch.await(1, TimeUnit.SECONDS);
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify
	//		List<Event> events = eventHub.getEvents();
	//		assertEquals(2, events.size());
	//		HashMap<String, String> aamProfile = (HashMap<String, String>) audienceResponse.get(RESPONSE_PROFILE_DATA);
	//		assertTrue(aamProfile.isEmpty());
	//	}
	//
	//	@Test
	//	public void testAudienceReset_shouldClearAllAudienceData() throws Exception {
	//		// Preset the shared state for audience manager
	//		mockUUIDInPersistence();
	//		mockVisitorProfileInPersistence();
	//
	//		eventHub.ignoreAllStateChangeEvents();
	//		EventData aamSharedState = new EventData();
	//		aamSharedState.putString(UUID_KEY, "testUUID");
	//		aamSharedState.putString(DPID_KEY, "testDPID");
	//		aamSharedState.putString(DPUUID_KEY, "testDPUUID");
	//		aamSharedState.putStringMap(
	//			VISITOR_PROFILE_KEY,
	//			new HashMap<String, String>() {
	//				{
	//					put("key1", "val1");
	//				}
	//			}
	//		);
	//		eventHub.createSharedState("com.adobe.module.audience", 0, aamSharedState);
	//
	//		eventHub.clearIgnoredEventFilters();
	//		eventHub.setExpectedEventCount(1);
	//		eventHub.ignoreEvents(EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET);
	//		// Test
	//		eventHub.dispatch(
	//			new Event.Builder("AudienceRequestReset", EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET).build()
	//		);
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		List<Event> events = eventHub.getEvents();
	//
	//		assertEquals(1, events.size());
	//		assertNull(platformServices.getLocalStorageService().getDataStore(AAM_DATA_STORE).getString("AAMUserId", null));
	//		assertNull(
	//			platformServices.getLocalStorageService().getDataStore(AAM_DATA_STORE).getString("AAMUserProfile", null)
	//		);
	//		assertEquals(EventType.HUB, events.get(0).getEventType());
	//		assertEquals(EventSource.SHARED_STATE, events.get(0).getEventSource());
	//	}
	//
	//	@Test
	//	public void testLifecycleEventToAAM_when_AAMForwardingEnabled_then_shouldNotSendRequest() {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(1);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingEnabled();
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//	}
	//
	//	@Test
	//	public void testLifecycleEventToAAM_when_AAMForwardingDisabled_then_shouldSendRequest() throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(1);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingDisabled();
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify
	//		assertEquals(1, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_contextDataKey=contextDataValue"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_mid=marketingCloudId"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_blob=blobValue"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("dcs_region=locationHintValue"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_uuid=testUUID"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//	}
	//
	//	@Test
	//	public void testSubmitSignalTwice_then_ConfigUpdate_shouldSendRequestTwiceInOrder() throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(2);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// dispatch Submit Event 1
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, null));
	//
	//		// dispatch Submit Event 2
	//		HashMap<String, String> data2 = new HashMap<String, String>();
	//		data2.put("key2", "value2");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data2, null));
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify that no network call is made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		testableNetworkService.setExpectedCount(2);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingDisabled();
	//
	//		// Verify that two network calls are made
	//		assertEquals(2, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_key1=value1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//
	//		assertTrue(testableNetworkService.getItem(1).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("c_key2=value2"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(1).connectPayload);
	//		assertNull(testableNetworkService.getItem(1).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(1).type);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_then_LifecycleEventToAAM_then_ConfigUpdate_with_AAMForwardingEnabled_then_shouldSendRequestOnce()
	//		throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(2);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// dispatch Submit Event 1
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, null));
	//
	//		// dispatch Lifecycle Event
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify that no network call is made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		testableNetworkService.setExpectedCount(2);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingEnabled();
	//
	//		// Verify that one network call is made
	//		assertEquals(1, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_key1=value1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_then_LifecycleEventToAAM_then_IdentityUpdate_with_AAMForwardingEnabled_then_shouldSendRequestOnce()
	//		throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(2);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingEnabled();
	//
	//		// create pending Identity state (causes AAM to wait)
	//		eventHub.createSharedState("com.adobe.module.identity", 0, EventHub.SHARED_STATE_PENDING);
	//
	//		// dispatch Submit Event 1
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, null));
	//
	//		// dispatch Lifecycle Event
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify that no network call is made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		testableNetworkService.setExpectedCount(1);
	//
	//		// update pending Identity state
	//		eventHub.updateSharedState(
	//			"com.adobe.module.identity",
	//			0,
	//			new EventData()
	//				.putString("mid", "marketingCloudId")
	//				.putString("blob", "blobValue")
	//				.putString("locationhint", "locationHintValue")
	//		);
	//
	//		// Verify that one network call is made
	//		assertEquals(1, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_key1=value1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_then_LifecycleEventToAAM_then_ConfigUpdate_with_AAMForwardingDisabled_then_shouldSendRequestTwice()
	//		throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(2);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// dispatch Submit Event 1
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, null));
	//
	//		// dispatch Lifecycle Event
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify that no network call is made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		testableNetworkService.setExpectedCount(2);
	//
	//		// configure
	//		configureWithPrivacyAAMForwardingDisabled();
	//
	//		// Verify that two network calls are made
	//		assertEquals(2, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_key1=value1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//
	//		assertTrue(testableNetworkService.getItem(1).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("c_contextDataKey=contextDataValue"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(1).connectPayload);
	//		assertNull(testableNetworkService.getItem(1).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(1).type);
	//	}
	//
	//	@Test
	//	public void testLifecycleEvent_then_SubmitSignal_then_ConfigUpdate_with_then_shouldSendRequestTwice()
	//		throws Exception {
	//		// setup expectations
	//		testableNetworkService.setExpectedCount(2);
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// dispatch Lifecycle Event
	//		eventHub.dispatch(createLifecycleResponseEvent());
	//
	//		// dispatch Submit Event 1
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, null));
	//
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify that no network call is made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		testableNetworkService.setExpectedCount(2);
	//
	//		// configure
	//		providedValidConfigurationState();
	//
	//		// Verify that two network calls are made in order
	//		assertEquals(2, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("c_contextDataKey=contextDataValue"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(0).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(0).connectPayload);
	//		assertNull(testableNetworkService.getItem(0).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(0).type);
	//
	//		assertTrue(testableNetworkService.getItem(1).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("c_key1=value1"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_ptfm=mockPlatform"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_dst=1"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("d_rtbd=json"));
	//		assertNull(testableNetworkService.getItem(1).connectPayload);
	//		assertNull(testableNetworkService.getItem(1).requestProperty);
	//		assertEquals(TestableNetworkService.NetworkRequestType.SYNC, testableNetworkService.getItem(1).type);
	//		waitForThreadsWithFailIfTimedOut(1000);
	//	}
	//
	//	@Test
	//	public void testSharedState_When_EverythingIsSet() throws Exception {
	//		// setup expectations
	//		eventHub.ignoreAllStateChangeEvents();
	//		eventHub.ignoreEvents(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT);
	//
	//		// dispatch setter for dpid and dpuuid
	//		eventHub.dispatch(createAudienceRequestIdentityEvent("testdpid", "testDpuuid"));
	//
	//		// Preset the shared state and shared Preferences
	//		providedValidConfigurationState();
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//		testableNetworkService.setDefaultResponse(
	//			"{\"uuid\":\"19994521975870785742420741570375407533\", \"dests\":[{\"c\":\"http://someurl.com/forward\"}], \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
	//		);
	//
	//		// dispatch audience request event
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(sampleCustomerData(), null));
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// verify shared state
	//		HashMap<String, String> aamProfileResponse = new HashMap<String, String>();
	//		aamProfileResponse.put("cn", "cv");
	//		EventData sharedState = getLastAudienceSharedState();
	//		assertEquals(aamProfileResponse, sharedState.optStringMap("aamprofile", null));
	//		assertEquals("testdpid", sharedState.optString("dpid", null));
	//		assertEquals("testDpuuid", sharedState.optString("dpuuid", null));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_LocalStorageService_NotInitialized_ShouldNotCrash() throws Exception {
	//		// recreate eventHub with null DataStorageService.
	//		final CountDownLatch latch = new CountDownLatch(1);
	//
	//		eventHub.shutdown();
	//		platformServices = new ModuleTestPlatformServices();
	//		platformServices.setFakeLocalStorageService(null);
	//		eventHub = new MockEventHubModuleTest("eventhub", platformServices);
	//		eventHub.registerModule(AudienceExtension.class);
	//
	//		systemInfoService = (MockSystemInfoService) platformServices.getSystemInfoService();
	//		systemInfoService.networkConnectionStatus = SystemInfoService.ConnectionStatus.CONNECTED;
	//		testableNetworkService = (TestableNetworkService) platformServices.getNetworkService();
	//		loggingService = (FakeLoggingService) platformServices.getLoggingService();
	//		Log.setLoggingService(loggingService);
	//		Log.setLogLevel(LoggingMode.VERBOSE);
	//
	//		// Setup
	//		eventHub.setExpectedEventCount(3);
	//		eventHub.ignoreEvents(EventType.HUB, EventSource.SHARED_STATE);
	//		final HashMap<String, Object> audienceResponse = new HashMap<String, Object>();
	//		testableNetworkService.setExpectedCount(2);
	//		testableNetworkService.setDefaultResponse(
	//			"{\"uuid\":\"19994521975870785742420741570375407533\", \"dests\":[{\"c\":\"http://www.google.com\"}], \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
	//		);
	//
	//		// Preset the shared state and shared Preferences
	//		providedValidConfigurationState();
	//		providedValidIdentityState();
	//
	//		// Test
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		AdobeCallback<HashMap<String, String>> callback = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback));
	//		latch.await(2, TimeUnit.SECONDS);
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify
	//		List<Event> events = eventHub.getEvents();
	//		assertEquals(3, events.size());
	//		HashMap<String, String> aamProfile = (HashMap<String, String>) audienceResponse.get(RESPONSE_PROFILE_DATA);
	//		assertEquals("cv", aamProfile.get("cn"));
	//
	//		assertTrue(
	//			loggingService.containsErrorLog(
	//				"Audience Manager",
	//				"LocalStorage service was not initialized, unable to retrieve uuid from persistence"
	//			)
	//		);
	//		assertTrue(
	//			loggingService.containsErrorLog(
	//				"Audience Manager",
	//				"LocalStorage service was not initialized, unable to update uuid in persistence"
	//			)
	//		);
	//		assertTrue(
	//			loggingService.containsErrorLog(
	//				"Audience Manager",
	//				"LocalStorage service was not initialized, unable to update visitor profile in persistence"
	//			)
	//		);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_DatabaseService_NotInitialized_ShouldNotCrash() throws Exception {
	//		// recreate eventHub with null DataStorageService.
	//		eventHub.shutdown();
	//		platformServices = new ModuleTestPlatformServices();
	//		platformServices.setMockStructuredDataService(null);
	//		loggingService = (FakeLoggingService) platformServices.getLoggingService();
	//		Log.setLoggingService(loggingService);
	//		Log.setLogLevel(LoggingMode.VERBOSE);
	//		eventHub = new MockEventHubModuleTest("eventhub", platformServices);
	//		eventHub.registerModule(AudienceExtension.class);
	//		systemInfoService = (MockSystemInfoService) platformServices.getSystemInfoService();
	//		systemInfoService.networkConnectionStatus = SystemInfoService.ConnectionStatus.CONNECTED;
	//		testableNetworkService = (TestableNetworkService) platformServices.getNetworkService();
	//
	//		// Setup
	//		eventHub.setExpectedEventCount(3);
	//		eventHub.ignoreEvents(EventType.HUB, EventSource.SHARED_STATE);
	//		final HashMap<String, Object> audienceResponse = new HashMap<String, Object>();
	//		final CountDownLatch latch = new CountDownLatch(1);
	//		testableNetworkService.setExpectedCount(2);
	//		testableNetworkService.setDefaultResponse(
	//			"{\"uuid\":\"19994521975870785742420741570375407533\", \"dests\":[{\"c\":\"http://www.google.com\"}], \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
	//		);
	//
	//		// Preset the shared state and shared Preferences
	//		providedValidConfigurationState();
	//		providedValidIdentityState();
	//
	//		// Test
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		AdobeCallback<HashMap<String, String>> callback = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback));
	//		latch.await(2, TimeUnit.SECONDS);
	//		waitForThreadsWithFailIfTimedOut(5000);
	//
	//		// Verify
	//		// No network calls are made
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		assertTrue(
	//			loggingService.containsErrorLog(
	//				"Audience Manager",
	//				"Database Service is not available, AAM dispatchers and Listeners will not be registered"
	//			)
	//		);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_PrivacyUnknown_Then_PrivacyChangesToOptIN_ShouldSendTwoHits() throws Exception {
	//		eventHub.ignoreEvents(EventType.HUB, EventSource.SHARED_STATE);
	//		final HashMap<String, Object> audienceResponse1 = new HashMap<String, Object>();
	//		final HashMap<String, Object> audienceResponse2 = new HashMap<String, Object>();
	//		final CountDownLatch latch = new CountDownLatch(2);
	//		testableNetworkService.setExpectedCount(2);
	//		testableNetworkService.setDefaultResponse(
	//			"{\"uuid\":\"19994521975870785742420741570375407533\", \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
	//		);
	//
	//		// Preset the shared state and shared Preferences
	//		configureWithPrivacy("optunknown", 0);
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//
	//		// Test
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		AdobeCallback<HashMap<String, String>> callback1 = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse1.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//		AdobeCallback<HashMap<String, String>> callback2 = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse2.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback1));
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback2));
	//
	//		latch.await(1, TimeUnit.SECONDS);
	//		waitForThreadsWithFailIfTimedOut(1000);
	//
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//
	//		// Privacy changes to opt in
	//		dispatchConfigurationResponseEvent(MobilePrivacyStatus.OPT_IN);
	//		assertEquals(2, testableNetworkService.waitAndGetCount());
	//		assertTrue(testableNetworkService.getItem(0).url.contains("http://server/event?"));
	//		assertTrue(testableNetworkService.getItem(1).url.contains("http://server/event?"));
	//
	//		waitForThreadsWithFailIfTimedOut(1000);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_PrivacyUnknown_Then_PrivacyChangesToOptOUT_ShouldClearHits() throws Exception {
	//		eventHub.ignoreEvents(EventType.HUB, EventSource.SHARED_STATE);
	//		final HashMap<String, Object> audienceResponse1 = new HashMap<String, Object>();
	//		final HashMap<String, Object> audienceResponse2 = new HashMap<String, Object>();
	//		final CountDownLatch latch = new CountDownLatch(2);
	//		testableNetworkService.setExpectedCount(2);
	//		testableNetworkService.setDefaultResponse(
	//			"{\"uuid\":\"19994521975870785742420741570375407533\", \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
	//		);
	//
	//		// Preset the shared state and shared Preferences
	//		configureWithPrivacy("optunknown", 0);
	//		providedValidIdentityState();
	//		mockUUIDInPersistence();
	//
	//		// Test
	//		HashMap<String, String> data = new HashMap<String, String>();
	//		data.put("key1", "value1");
	//		AdobeCallback<HashMap<String, String>> callback1 = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse1.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//		AdobeCallback<HashMap<String, String>> callback2 = new AdobeCallback<HashMap<String, String>>() {
	//			@Override
	//			public void call(HashMap<String, String> profileData) {
	//				audienceResponse2.put(RESPONSE_PROFILE_DATA, profileData);
	//				latch.countDown();
	//			}
	//		};
	//
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback1));
	//		eventHub.dispatch(createAudienceRequestContentEventWithData(data, callback2));
	//
	//		latch.await(1, TimeUnit.SECONDS);
	//		waitForThreadsWithFailIfTimedOut(1000);
	//
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//
	//		// Privacy changes to opt out
	//		//The only hit that goes out is the opt-out hit
	//		dispatchConfigurationResponseEvent(MobilePrivacyStatus.OPT_OUT);
	//		assertEquals(1, testableNetworkService.waitAndGetCount());
	//		assertEquals(
	//			"The Network hit should have been the AAM optout hit!",
	//			"https://server/demoptout.jpg?d_uuid=testUUID",
	//			testableNetworkService.getItem(0).url
	//		);
	//		waitForThreadsWithFailIfTimedOut(1000);
	//		testableNetworkService.clearNetworkRequests();
	//
	//		// Privacy change to optin and check that the previous events are cleared on opt-out
	//		dispatchConfigurationResponseEvent(MobilePrivacyStatus.OPT_IN);
	//		assertEquals(0, testableNetworkService.waitAndGetCount());
	//		waitForThreadsWithFailIfTimedOut(1000);
	//	}
	//
	@Test
	public void test_RulesResponseEvent_when_NotAAM_then_shouldNotSendRequest() {
		// setup expectations
		testableNetworkService.setExpectedNetworkRequest(
			new TestableNetworkRequest("https://audience.server", HttpMethod.GET),
			1
		);

		// configure
		providedValidConfigurationState();
		mockUUIDInPersistence();

		MobileCore.dispatchEvent(createNonAAMRulesResponseEvent());

		// Verify
		assertTrue(
			testableNetworkService
				.getReceivedNetworkRequestsMatching(
					new TestableNetworkRequest("https://audience.server", HttpMethod.GET)
				)
				.isEmpty()
		);
	}

	// =============================================================================
	// Configuration Shared State Mocks
	// =============================================================================
	private void providedValidConfigurationState() {
		configureWithPrivacyAAMForwardingDisabled();
	}

	private void providedConfigurationStateWithoutAudienceManager() {
		MobileCore.clearUpdatedConfiguration();
	}

	private void configureWithPrivacy(final String privacyStatus) {
		Map<String, Object> config = new HashMap<>();
		config.put("audience.server", "server");
		config.put("audience.timeout", 5);
		config.put("global.privacy", privacyStatus);
		config.put("analytics.aamForwardingEnabled", false);
		config.put("experienceCloud.org", "test@AdobeOrg");

		MobileCore.updateConfiguration(config);
	}

	private void configureWithPrivacyAAMForwardingDisabled() {
		Map<String, Object> config = new HashMap<>();
		config.put("audience.server", "server");
		config.put("audience.timeout", 5);
		config.put("global.privacy", "optedin");
		config.put("analytics.aamForwardingEnabled", false);
		config.put("experienceCloud.org", "test@AdobeOrg");

		MobileCore.updateConfiguration(config);
	}

	private void configureWithPrivacyAAMForwardingEnabled() {
		Map<String, Object> config = new HashMap<>();
		config.put("audience.server", "server");
		config.put("audience.timeout", 5);
		config.put("global.privacy", "optedin");
		config.put("analytics.aamForwardingEnabled", true);
		config.put("experienceCloud.org", "test@AdobeOrg");

		MobileCore.updateConfiguration(config);
	}

	// =============================================================================
	// Persistence Mocks
	// =============================================================================

	private void mockUUIDInPersistence() {
		TestPersistenceHelper.updatePersistence(
			AudienceTestConstants.DataStoreKey.AUDIENCE_DATASTORE,
			"AAMUserId",
			"testUUID"
		);
	}

	private void mockVisitorProfileInPersistence() {
		HashMap<String, String> visitorProfileMap = new HashMap<>();
		visitorProfileMap.put("visitorKey", "visitorValue");
		TestPersistenceHelper.updatePersistence(
			AudienceTestConstants.DataStoreKey.AUDIENCE_DATASTORE,
			"AAMUserProfile",
			visitorProfileMap
		);
	}

	// =============================================================================
	// Mock Events
	// =============================================================================

	private Event createAudienceRequestIdentityEvent(final String dpid, final String dpuuid) {
		final Map<String, Object> data = new HashMap<>();
		data.put(AudienceConstants.EventDataKeys.Audience.DPID, StringUtils.isNullOrEmpty(dpid) ? "" : dpid);
		data.put(AudienceConstants.EventDataKeys.Audience.DPUUID, StringUtils.isNullOrEmpty(dpuuid) ? "" : dpuuid);
		final Event event = new Event.Builder(
			"AudienceSetDataProviderIds",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(data)
			.build();
		return event;
	}

	private Event createAudienceRequestIdentityEvent(final AdobeCallbackWithError<Map<String, Object>> callback) {
		final Event event = new Event.Builder(
			"AudienceGetDataProviderIds",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_IDENTITY
		)
			.build();

		MobileCore.dispatchEventWithResponseCallback(
			event,
			500,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(AdobeError adobeError) {
					callback.fail(adobeError);
				}

				@Override
				public void call(Event event) {
					final Map<String, Object> eventData = event.getEventData();
					callback.call(eventData);
				}
			}
		);
		return event;
	}

	private Event createAudienceRequestContentEventWithData(
		final HashMap<String, String> data,
		final AdobeCallbackWithError<Map<String, String>> callback
	) {
		final Event event = new Event.Builder(
			"AudienceRequestContent",
			EventType.AUDIENCEMANAGER,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(AudienceConstants.EventDataKeys.Audience.VISITOR_TRAITS, data);
					}
				}
			)
			.build();

		if (callback != null) {
			MobileCore.dispatchEventWithResponseCallback(
				event,
				500,
				new AdobeCallbackWithError<Event>() {
					@Override
					public void fail(AdobeError adobeError) {
						callback.fail(adobeError);
					}

					@Override
					public void call(final Event e) {
						final Map<String, Object> eventData = e.getEventData();
						callback.call(
							DataReader.optStringMap(
								eventData,
								AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE,
								null
							)
						);
					}
				}
			);
		}

		return event;
	}

	private Event createLifecycleResponseEvent() {
		Map<String, Object> eventData = new HashMap<>();
		HashMap<String, String> data = new HashMap<>();
		data.put("contextDataKey", "contextDataValue");
		eventData.put("lifecyclecontextdata", data);
		final Event event = new Event.Builder(
			"Lifecycle launch event",
			EventType.LIFECYCLE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(eventData)
			.build();
		return event;
	}

	private Event createAAMRulesResponseEvent() {
		Map<String, Object> eventData = new HashMap<>();
		HashMap<String, String> data = new HashMap<>();
		data.put("RulesTraitKey", "RulesTraitValue");
		eventData.put("audience_manager_data", data);
		final Event event = new Event.Builder("Rules Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
			.setEventData(eventData)
			.build();
		return event;
	}

	private Event createNonAAMRulesResponseEvent() {
		Map<String, Object> eventData = new HashMap<>();
		HashMap<String, String> data = new HashMap<>();
		data.put("RulesTraitKey", "RulesTraitValue");
		eventData.put("non_audience_manager_data", data);
		final Event event = new Event.Builder("Rules Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
			.setEventData(eventData)
			.build();
		return event;
	}

	private HashMap<String, String> sampleCustomerData() {
		HashMap<String, String> data = new HashMap<>();
		data.put("customerDataKey", "customerDataValue");
		return data;
	}

	private Map<String, Object> getLastAudienceSharedState() {
		try {
			return TestHelper.getSharedStateFor("com.adobe.module.audience", 5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	void dispatchConfigurationResponseEvent(final MobilePrivacyStatus status) {
		Map<String, Object> config = new HashMap<>();
		config.put("audience.server", "server");
		config.put("global.privacy", status.getValue());
		config.put("audience.timeout", 5);
		config.put("analytics.aamForwardingEnabled", true);
		final Event event = new Event.Builder(
			"Configuration Response Event",
			EventType.CONFIGURATION,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(config)
			.build();
		// todo: set shared state
		//eventHub.createSharedState("com.adobe.module.configuration", eventHub.getAllEventsCount(), config);
		MobileCore.dispatchEvent(event);
	}
}
