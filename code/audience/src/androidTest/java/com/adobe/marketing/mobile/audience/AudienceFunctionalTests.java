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
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Audience;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
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
public class AudienceFunctionalTests {

	private final Map<String, String> signalData = new HashMap<String, String>() {
		{
			put("key1", "value1");
		}
	};
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
	public void testSubmitSignal_when_NetworkHasUnrecoverableError_thenCallbackCalledWithEmptyProfile()
		throws Exception {
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setResponseConnectionFor(signalRequest, new MockConnection(404, null));
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 1);

		mockUUIDInPersistence();
		registerExtensions(config);

		final HashMap<String, String> responseProfile = new HashMap<>();
		final CountDownLatch latch = new CountDownLatch(1);

		// Test
		Audience.signalWithData(
			signalData,
			profileData -> {
				responseProfile.putAll(profileData);
				latch.countDown();
			}
		);

		// verify
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
	public void testLifecycleResponseEvent_AndAAMForwardingEnabled_thenShouldNotSendRequest() {
		// setup
		testableNetworkService.setExpectedNetworkRequest(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET),
			1
		);

		mockUUIDInPersistence();
		config.put("analytics.aamForwardingEnabled", true);
		registerExtensions(config);

		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// verify
		assertEquals(
			0,
			testableNetworkService
				.getReceivedNetworkRequestsMatching(new TestableNetworkRequest("https://server/event", HttpMethod.GET))
				.size()
		);
	}

	@Ignore("to investigate, fails when running the entire suite")
	@Test
	public void testLifecycleResponseEvent_AndAAMForwardingDisabled_thenShouldSendRequest() throws Exception {
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

		// verify
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
		Map<String, String> data1 = new HashMap<>();
		data1.put("key1", "value1");
		Audience.signalWithData(data1, null);

		// dispatch Submit Event 2
		HashMap<String, String> data2 = new HashMap<>();
		data2.put("key2", "value2");
		Audience.signalWithData(data2, null);

		// verify that no network call is made
		List<TestableNetworkRequest> networkRequests = testableNetworkService.getReceivedNetworkRequestsMatching(
			new TestableNetworkRequest("https://server/event", HttpMethod.GET)
		);
		assertEquals(0, networkRequests.size());

		// configure
		config.put("analytics.aamForwardingEnabled", false);
		MobileCore.updateConfiguration(config);

		// verify that two network calls are made
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

	@Ignore("fails in suite")
	@Test
	public void testSubmitSignal_AndLifecycleResponseEvent_AndConfigWithAAMForwardingEnabled_thenShouldSendSignalAndIgnoreLifecycle()
		throws Exception {
		// setup
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 1);

		config.put("analytics.aamForwardingEnabled", true);
		registerExtensions(config);

		// dispatch signal
		Audience.signalWithData(signalData, null);

		// dispatch Lifecycle Event
		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// verify that two network calls are made in order
		testableNetworkService.assertNetworkRequestCount();
		List<TestableNetworkRequest> requests = testableNetworkService.getReceivedNetworkRequestsMatching(
			signalRequest
		);
		assertEquals(1, requests.size());
		assertTrue(requests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(0).getUrl().contains("c_key1=value1"));
		assertTrue(requests.get(0).getUrl().contains("d_ptfm=android"));
		assertTrue(requests.get(0).getUrl().contains("d_dst=1"));
		assertTrue(requests.get(0).getUrl().contains("d_rtbd=json"));
		assertNull(requests.get(0).getBody());
		assertNull(requests.get(0).getHeaders());
	}

	@Ignore("fails when running the entire suite")
	@Test
	public void testSubmitSignal_AndLifecycleResponseEvent_AndConfigWithAAMForwardingDisabled_thenShouldSendTwoRequestsInCorrectOrder()
		throws Exception {
		// setup
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 2);

		config.put("analytics.aamForwardingEnabled", false);
		registerExtensions(config);

		// dispatch signal
		Audience.signalWithData(signalData, null);

		// dispatch Lifecycle Event
		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// verify that two network calls are made in order
		testableNetworkService.assertNetworkRequestCount();
		List<TestableNetworkRequest> requests = testableNetworkService.getReceivedNetworkRequestsMatching(
			signalRequest
		);
		assertEquals(2, requests.size());
		assertTrue(requests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(0).getUrl().contains("c_key1=value1"));
		assertTrue(requests.get(0).getUrl().contains("d_ptfm=android"));
		assertTrue(requests.get(0).getUrl().contains("d_dst=1"));
		assertTrue(requests.get(0).getUrl().contains("d_rtbd=json"));
		assertNull(requests.get(0).getBody());
		assertNull(requests.get(0).getHeaders());

		assertTrue(requests.get(1).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(1).getUrl().contains("c_contextDataKey=contextDataValue"));
		assertTrue(requests.get(1).getUrl().contains("d_ptfm=android"));
		assertTrue(requests.get(1).getUrl().contains("d_dst=1"));
		assertTrue(requests.get(1).getUrl().contains("d_rtbd=json"));
		assertNull(requests.get(1).getBody());
		assertNull(requests.get(1).getHeaders());
	}

	@Ignore("fails when running the entire suite")
	@Test
	public void testLifecycleEvent_AndSubmitSignal_AndConfigUpdate_thenShouldSendTwoRequestsInCorrectOrder()
		throws InterruptedException {
		// setup
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 2);

		config.put("global.privacy", "optunknown");
		registerExtensions(config);

		// dispatch Lifecycle Event
		MobileCore.dispatchEvent(createLifecycleResponseEvent());

		// dispatch signal
		Audience.signalWithData(signalData, null);

		// verify that no network call is made
		assertEquals(0, testableNetworkService.getReceivedNetworkRequestsMatching(signalRequest).size());

		// configure
		config.put("global.privacy", "optedin");
		MobileCore.updateConfiguration(config);

		// verify that two network calls are made in order
		testableNetworkService.assertNetworkRequestCount();
		List<TestableNetworkRequest> requests = testableNetworkService.getReceivedNetworkRequestsMatching(
			signalRequest
		);
		assertEquals(2, requests.size());
		assertTrue(requests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(0).getUrl().contains("c_contextDataKey=contextDataValue"));
		assertTrue(requests.get(0).getUrl().contains("d_ptfm=android"));
		assertTrue(requests.get(0).getUrl().contains("d_dst=1"));
		assertTrue(requests.get(0).getUrl().contains("d_rtbd=json"));
		assertNull(requests.get(0).getBody());
		assertNull(requests.get(0).getHeaders());

		assertTrue(requests.get(1).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(1).getUrl().contains("c_key1=value1"));
		assertTrue(requests.get(1).getUrl().contains("d_ptfm=android"));
		assertTrue(requests.get(1).getUrl().contains("d_dst=1"));
		assertTrue(requests.get(1).getUrl().contains("d_rtbd=json"));
		assertNull(requests.get(1).getBody());
		assertNull(requests.get(1).getHeaders());
	}

	@Ignore("investigate: Cannot create com.adobe.module.audience shared state at version 4. More recent state exists.")
	@Test
	public void testSubmitSignal_WhenEverythingIsSetAndServerResponse_updatesSharedState() throws Exception {
		// setup
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setResponseConnectionFor(
			signalRequest,
			new MockConnection(
				200,
				"{\"uuid\":\"19994521975870785742420741570375407533\", \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
			)
		);
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 1);

		// preset the config and shared Preferences
		//mockUUIDInPersistence();
		registerExtensions(config);

		// test
		Audience.signalWithData(signalData, null);

		// verify shared state
		testableNetworkService.assertNetworkRequestCount();
		TestHelper.sleep(200);
		Map<String, String> expectedProfile = new HashMap<>();
		expectedProfile.put("cn", "cv");

		Map<String, Object> sharedState = getLastAudienceSharedState();
		assertNotNull(sharedState);
		assertEquals(2, sharedState.size());
		assertEquals(expectedProfile, DataReader.optStringMap(sharedState, "aamprofile", null));
		assertEquals("testUUID", DataReader.optString(sharedState, "uuid", null));
	}

	@Ignore("fails when running in the suite")
	@Test
	public void testSubmitSignal_when_PrivacyUnknown_Then_PrivacyChangesToOptIN_ShouldSendTwoHits() throws Exception {
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setResponseConnectionFor(
			signalRequest,
			new MockConnection(
				200,
				"{\"uuid\":\"19994521975870785742420741570375407533\", \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
			)
		);
		testableNetworkService.setExpectedNetworkRequest(signalRequest, 2);

		// Preset the config and shared Preferences
		config.put("global.privacy", "optunknown");
		mockUUIDInPersistence();
		registerExtensions(config);

		// Test
		final HashMap<String, String> audienceProfileResponse1 = new HashMap<>();
		final HashMap<String, String> audienceProfileResponse2 = new HashMap<>();
		Map<String, String> data1 = new HashMap<>();
		data1.put("key1", "value1");

		final CountDownLatch latch = new CountDownLatch(2);
		Audience.signalWithData(
			data1,
			profileData -> {
				audienceProfileResponse1.putAll(profileData);
				latch.countDown();
			}
		);

		HashMap<String, String> data2 = new HashMap<>();
		data2.put("key2", "value2");
		Audience.signalWithData(
			data2,
			profileData -> {
				audienceProfileResponse2.putAll(profileData);
				latch.countDown();
			}
		);

		latch.await(1, TimeUnit.SECONDS);
		assertTrue(audienceProfileResponse1.isEmpty());
		assertTrue(audienceProfileResponse2.isEmpty());

		assertEquals(0, testableNetworkService.getReceivedNetworkRequestsMatching(signalRequest).size());

		// Privacy changes to opt in
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
		testableNetworkService.assertNetworkRequestCount();
		List<TestableNetworkRequest> requests = testableNetworkService.getReceivedNetworkRequestsMatching(
			signalRequest
		);
		assertEquals(2, requests.size());
		assertTrue(requests.get(0).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(0).getUrl().contains("c_key1=value1"));
		assertTrue(requests.get(1).getUrl().contains("https://server/event?"));
		assertTrue(requests.get(1).getUrl().contains("c_key2=value2"));
	}

	@Test
	public void testSubmitSignal_when_PrivacyUnknown_Then_PrivacyChangesToOptOUT_ShouldClearHits() throws Exception {
		TestableNetworkRequest signalRequest = new TestableNetworkRequest("https://server/event", HttpMethod.GET);
		testableNetworkService.setResponseConnectionFor(
			signalRequest,
			new MockConnection(
				200,
				"{\"uuid\":\"19994521975870785742420741570375407533\", \"stuff\":[{\"cv\":\"cv\",\"cn\":\"cn\"}]}"
			)
		);

		TestableNetworkRequest optoutRequest = new TestableNetworkRequest(
			"https://server/demoptout.jpg",
			HttpMethod.GET
		);
		testableNetworkService.setExpectedNetworkRequest(optoutRequest, 1);

		// Preset config and shared Preferences
		config.put("global.privacy", "optunknown");
		mockUUIDInPersistence();
		registerExtensions(config);

		// Test
		final HashMap<String, String> audienceProfileResponse1 = new HashMap<>();
		final HashMap<String, String> audienceProfileResponse2 = new HashMap<>();

		final CountDownLatch latch = new CountDownLatch(2);
		Audience.signalWithData(
			signalData,
			profileData -> {
				audienceProfileResponse1.putAll(profileData);
				latch.countDown();
			}
		);
		Audience.signalWithData(
			signalData,
			profileData -> {
				audienceProfileResponse2.putAll(profileData);
				latch.countDown();
			}
		);

		latch.await(1, TimeUnit.SECONDS);
		assertTrue(audienceProfileResponse1.isEmpty());
		assertTrue(audienceProfileResponse2.isEmpty());

		assertEquals(0, testableNetworkService.getReceivedNetworkRequestsMatching(signalRequest).size());

		// Privacy changes to opt out, the only hit that goes out is the opt-out hit
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);

		testableNetworkService.assertNetworkRequestCount();
		assertEquals(0, testableNetworkService.getReceivedNetworkRequestsMatching(signalRequest).size());
		assertEquals(1, testableNetworkService.getReceivedNetworkRequestsMatching(optoutRequest).size());
		assertEquals(
			"The Network hit should have been the AAM optout hit!",
			"https://server/demoptout.jpg?d_uuid=testUUID",
			testableNetworkService.getReceivedNetworkRequestsMatching(optoutRequest).get(0).getUrl()
		);

		// Privacy change to optin and check that the previous events are cleared on opt-out
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
		assertEquals(0, testableNetworkService.getReceivedNetworkRequestsMatching(signalRequest).size());
	}

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

		// verify
		assertTrue(
			testableNetworkService
				.getReceivedNetworkRequestsMatching(
					new TestableNetworkRequest("https://audience.server", HttpMethod.GET)
				)
				.isEmpty()
		);
	}

	// =============================================================================
	// Persistence mocks
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
	// Event and extension helpers
	// =============================================================================
	private Event createLifecycleResponseEvent() {
		Map<String, Object> eventData = new HashMap<>();
		Map<String, String> data = new HashMap<>();
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

	private Event createNonAAMRulesResponseEvent() {
		Map<String, Object> eventData = new HashMap<>();
		Map<String, String> data = new HashMap<>();
		data.put("RulesTraitKey", "RulesTraitValue");
		eventData.put("non_audience_manager_data", data);
		return new Event.Builder("Rules Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
			.setEventData(eventData)
			.build();
	}

	private Map<String, Object> getLastAudienceSharedState() {
		try {
			return TestHelper.getSharedStateFor("com.adobe.module.audience", 5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
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
}
