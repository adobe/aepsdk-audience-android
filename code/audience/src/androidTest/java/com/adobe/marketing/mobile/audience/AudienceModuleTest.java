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
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AudienceModuleTest {

	private final Map<String, Object> config = new HashMap<>();

	private TestableNetworkService testableNetworkService;

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	@Before
	public void beforeEach() {
		config.put("audience.server", "server");
		config.put("audience.timeout", 5);
		config.put("global.privacy", "optedin");
		config.put("analytics.aamForwardingEnabled", false);
		config.put("experienceCloud.org", "test@AdobeOrg");
		testableNetworkService = new TestableNetworkService();
		ServiceProvider.getInstance().setNetworkService(testableNetworkService);
		mockECIDInPersistence();
	}

	@After
	public void tearDown() {
		TestPersistenceHelper.resetKnownPersistence();
		testableNetworkService.reset();
		config.clear();
	}

	@Test
	public void testGetVisitorProfile_whenVisitorProfileInPersistence_returnsValue() throws Exception {
		// setup
		final CountDownLatch latch = new CountDownLatch(1);
		mockVisitorProfileInPersistence();
		registerExtensions(config);
		final Map<String, String> actualVisitorProfile = new HashMap<>();

		// test
		Audience.getVisitorProfile(
			new AdobeCallbackWithError<Map<String, String>>() {
				@Override
				public void fail(AdobeError adobeError) {
					Assert.fail("Unexpected AdobeError: " + adobeError.getErrorName());
				}

				@Override
				public void call(Map<String, String> eventData) {
					actualVisitorProfile.putAll(eventData);
					latch.countDown();
				}
			}
		);
		latch.await(1, TimeUnit.SECONDS);

		// verify
		assertEquals(1, actualVisitorProfile.size());
		assertEquals("visitorValue", actualVisitorProfile.get("visitorKey"));
	}

	@Ignore("to investigate, fails when running the entire suite")
	@Test
	public void testSubmitSignal_when_NetworkHasUnrecoverableError_then_callbackCalledWithEmptyProfile()
		throws Exception {
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setResponseConnectionFor(signalRequest, getMockConnection(404, null));
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 1);

		mockUUIDInPersistence();
		registerExtensions(config);

		final HashMap<String, String> responseProfile = new HashMap<>();
		final CountDownLatch latch = new CountDownLatch(1);

		// Test
		HashMap<String, String> data = new HashMap<>();
		data.put("key1", "value1");
		Audience.signalWithData(
			data,
			profileData -> {
				responseProfile.putAll(profileData);
				latch.countDown();
			}
		);

		// Verify
		testableNetworkService.assertNetworkRequestCount();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertTrue(responseProfile.isEmpty());
	}

	@Test
	public void testAudienceReset_shouldClearAllAudienceData() {
		// setup
		mockUUIDInPersistence();
		mockVisitorProfileInPersistence();
		registerExtensions(config);

		// test
		Audience.reset();

		TestHelper.sleep(200);

		assertNull(
			TestPersistenceHelper.readPersistedData(AudienceTestConstants.DataStoreKey.AUDIENCE_DATASTORE, "AAMUserId")
		);
		assertNull(
			TestPersistenceHelper.readPersistedData(
				AudienceTestConstants.DataStoreKey.AUDIENCE_DATASTORE,
				"AAMUserProfile"
			)
		);
	}

	@Test
	public void testLifecycleEventToAAM_when_AAMForwardingEnabled_then_shouldNotSendRequest() {
		// setup
		testableNetworkService.setExpectedNetworkRequest(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET),
			1
		);

		mockUUIDInPersistence();
		config.put("analytics.aamForwardingEnabled", true);
		registerExtensions(config);

		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// Verify
		assertEquals(
			0,
			testableNetworkService
				.getReceivedNetworkRequestsMatching(new TestableNetworkRequest("https://server/event", HttpMethod.GET))
				.size()
		);
	}

	@Ignore("to investigate, fails when running the entire suite")
	@Test
	public void testLifecycleEventToAAM_when_AAMForwardingDisabled_then_shouldSendRequest() throws Exception {
		// setup
		testableNetworkService.setExpectedNetworkRequest(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET),
			1
		);

		mockUUIDInPersistence();
		config.put("analytics.aamForwardingEnabled", false);
		registerExtensions(config);

		// test
		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// Verify
		testableNetworkService.assertNetworkRequestCount();
		List<TestableNetworkRequest> networkRequests = testableNetworkService.getReceivedNetworkRequestsMatching(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET)
		);
		assertEquals(1, networkRequests.size());

		assertTrue(networkRequests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(networkRequests.get(0).getUrl().contains("c_contextDataKey=contextDataValue"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_mid=testMid"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_uuid=testUUID"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_ptfm=android"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_dst=1"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_rtbd=json"));
		assertNull(networkRequests.get(0).getBody());
		assertNull(networkRequests.get(0).getHeaders());
	}

	@Test
	public void testSubmitSignalTwice_then_ConfigUpdate_shouldSendRequestTwiceInOrder() throws Exception {
		// setup expectations
		testableNetworkService.setExpectedNetworkRequest(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET),
			2
		);
		mockUUIDInPersistence();
		registerExtensions(null);

		// test
		HashMap<String, String> data = new HashMap<>();
		data.put("key1", "value1");
		Audience.signalWithData(data, null);

		// dispatch Submit Event 2
		HashMap<String, String> data2 = new HashMap<>();
		data2.put("key2", "value2");
		Audience.signalWithData(data2, null);

		// Verify that no network call is made
		List<TestableNetworkRequest> networkRequests = testableNetworkService.getReceivedNetworkRequestsMatching(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET)
		);
		assertEquals(0, networkRequests.size());

		// configure
		config.put("analytics.aamForwardingEnabled", false);
		MobileCore.updateConfiguration(config);

		// Verify that two network calls are made
		testableNetworkService.assertNetworkRequestCount();
		networkRequests =
			testableNetworkService.getReceivedNetworkRequestsMatching(
				new TestableNetworkRequest("https://server/event", HttpMethod.GET)
			);
		assertEquals(2, networkRequests.size());
		assertTrue(networkRequests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(networkRequests.get(0).getUrl().contains("c_key1=value1"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_ptfm=android"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_dst=1"));
		assertTrue(networkRequests.get(0).getUrl().contains("d_rtbd=json"));
		assertNull(networkRequests.get(0).getBody());
		assertNull(networkRequests.get(0).getHeaders());

		assertTrue(networkRequests.get(1).getUrl().contains("https://server/event?"));
		assertTrue(networkRequests.get(1).getUrl().contains("c_key2=value2"));
		assertTrue(networkRequests.get(1).getUrl().contains("d_ptfm=android"));
		assertTrue(networkRequests.get(1).getUrl().contains("d_dst=1"));
		assertTrue(networkRequests.get(1).getUrl().contains("d_rtbd=json"));
		assertNull(networkRequests.get(1).getBody());
		assertNull(networkRequests.get(1).getHeaders());
	}

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
		mockUUIDInPersistence();
		registerExtensions(config);

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
	// Persistence Mocks
	// =============================================================================

	private void mockUUIDInPersistence() {
		TestPersistenceHelper.updatePersistence(
			AudienceTestConstants.DataStoreKey.AUDIENCE_DATASTORE,
			"AAMUserId",
			"testUUID"
		);
	}

	private void mockECIDInPersistence() {
		TestPersistenceHelper.updatePersistence(
			AudienceTestConstants.DataStoreKey.IDENTITY_DATASTORE,
			"ADOBEMOBILE_PERSISTED_MID",
			"testMid"
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

	void registerExtensions(final Map<String, Object> config) {
		try {
			TestHelper.registerExtensions(
				Arrays.asList(MonitorExtension.EXTENSION, Audience.EXTENSION, Identity.EXTENSION),
				config
			);
		} catch (InterruptedException e) {
			fail("Unexpected InterruptedException thrown on registerExtensions");
		}
	}

	HttpConnecting getMockConnection(final int responseCode, final String payload) {
		return new HttpConnecting() {
			@Override
			public InputStream getInputStream() {
				if (payload != null) {
					return new ByteArrayInputStream(payload.getBytes());
				}
				return null;
			}

			@Override
			public InputStream getErrorStream() {
				return null;
			}

			@Override
			public int getResponseCode() {
				return responseCode;
			}

			@Override
			public String getResponseMessage() {
				return null;
			}

			@Override
			public String getResponsePropertyValue(String s) {
				return null;
			}

			@Override
			public void close() {}
		};
	}
}
