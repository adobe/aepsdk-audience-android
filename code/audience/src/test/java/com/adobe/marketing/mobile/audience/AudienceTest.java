/* **************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2017 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 *
 **************************************************************************/
package com.adobe.marketing.mobile.audience;

import com.adobe.marketing.mobile.NetworkService.HttpCommand;
import com.adobe.marketing.mobile.JsonUtilityService.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AudienceTest extends BaseTest {
	private MockNetworkService                                    mockNetworkService;
	private LocalStorageService                                   fakeLocalStorageService;
	private JsonUtilityService                                    fakeJsonUtilityService;
	private MockDispatcherAudienceResponseContentAudienceManager  mockDispatcherAudienceResponseContent;
	private MockDispatcherAudienceResponseIdentityAudienceManager mockDispatcherAudienceResponseIdentity;
	private MockAudienceRequestsDatabase                          mockAudienceRequestsDatabase;
	private TestableAudience                                      audience;
	private AudienceState                                         state;

	@Before
	public void setup() throws MissingPlatformServicesException {
		super.beforeEach();
		// Mock Platform Services
		mockNetworkService = platformServices.getMockNetworkService();
		fakeJsonUtilityService = platformServices.getJsonUtilityService();
		fakeLocalStorageService = platformServices.getLocalStorageService();
		mockDispatcherAudienceResponseContent = new MockDispatcherAudienceResponseContentAudienceManager();
		mockDispatcherAudienceResponseIdentity = new MockDispatcherAudienceResponseIdentityAudienceManager();
		state = new AudienceState(fakeLocalStorageService);
		state.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audience = new TestableAudience(eventHub, platformServices, mockDispatcherAudienceResponseContent,
										mockDispatcherAudienceResponseIdentity, state);
		mockAudienceRequestsDatabase = (MockAudienceRequestsDatabase)audience.internalDatabase;
	}

	// =================================================================================================================
	// void bootup(final Event bootEvent)
	// =================================================================================================================
	@Test
	public void testBootup_when_UUID_Available() throws Exception {
		// setup
		final HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		state.setDpid("mock-dpid");
		state.setDpuuid("mock-dpuuid");
		state.setUuid("mock-uuid");
		state.setVisitorProfile(visitorProfile);

		Event bootEvent = new Event.Builder("AudienceTest", EventType.HUB, EventSource.BOOTED)
		.setEventNumber(0).build();

		// test
		audience.bootup(bootEvent);

		waitForExecutor(audience.getExecutor(), 1);

		// verify
		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(bootEvent);
		assertEquals(4, aamSharedState.size());
		assertEquals("mock-uuid", aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.UUID, ""));
		assertEquals("mock-dpid", aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPID, ""));
		assertEquals("mock-dpuuid", aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPUUID, ""));
		assertEquals(visitorProfile, aamSharedState.optStringMap(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
					 null));
	}

	@Test
	public void testBootup_when_UUID_NotAvailable() throws Exception {
		// setup
		Event bootEvent = new Event.Builder("AudienceTest", EventType.HUB, EventSource.BOOTED)
		.setEventNumber(0).build();

		// test
		audience.bootup(bootEvent);

		waitForExecutor(audience.getExecutor(), 1);

		// verify
		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(bootEvent);
		assertEquals(0, aamSharedState.size());
		assertNull(aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.UUID, null));
		assertNull(aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPID, null));
		assertNull(aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPUUID, null));
		assertNull(aamSharedState.optStringMap(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE, null));
	}

	// =================================================================================================================
	// void getIdentityVariables(final String pairId)
	// =================================================================================================================
	@Test
	public void testGetIdentityVariables_when_validPairID() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");

		audience.setDpidAndDpuuid("dpid", "dpuuid", testEvent);
		getAudienceDataStore().setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);

		// test
		audience.getIdentityVariables("pairId");

		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(1, mockDispatcherAudienceResponseIdentity.dispatchCallCount);
		assertEquals(visitorProfile, mockDispatcherAudienceResponseIdentity.dispatchParametersProfile);
		assertEquals("dpid", mockDispatcherAudienceResponseIdentity.dispatchParametersDpid);
		assertEquals("dpuuid", mockDispatcherAudienceResponseIdentity.dispatchParametersDpuuid);
	}

	@Test
	public void testGetIdentityVariables_when_pairIdNull() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		getAudienceDataStore().setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);
		audience.setDpidAndDpuuid("dpid", "dpuuid", testEvent);

		// test
		audience.getIdentityVariables(null);

		// verify
		assertFalse(mockDispatcherAudienceResponseIdentity.dispatchWasCalled);
		assertNull(mockDispatcherAudienceResponseIdentity.dispatchParametersPairId);
	}

	@Test
	public void testGetIdentityVariables_when_visitorProfileNotSet() throws Exception {
		// test
		audience.getIdentityVariables("pairId");

		waitForExecutor(audience.getExecutor(), 1);
		// verify
		assertEquals(1, mockDispatcherAudienceResponseIdentity.dispatchCallCount);
		assertNull(mockDispatcherAudienceResponseIdentity.dispatchParametersProfile);
		assertNull(mockDispatcherAudienceResponseIdentity.dispatchParametersDpid);
		assertNull(mockDispatcherAudienceResponseIdentity.dispatchParametersDpuuid);
	}

	// =================================================================================================================
	// void processStateChange(final String stateName, final Event event)
	// =================================================================================================================
	@Test
	public void testProcessChange_when_stateNameIsNull_then_shouldDoNothing() throws Exception {
		// setup
		final String testStateName = null;

		// test
		audience.processStateChange(testStateName);

		// verify
		assertFalse("Process Queued Events should not be called", audience.processQueuedEventsCalled);
	}

	@Test
	public void testProcessChange_when_stateNameIsConfiguration_then_shouldProcessQueuedEvents() throws Exception {

		// test
		audience.processStateChange(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME);

		waitForExecutor(audience.getExecutor(), 1);
		// verify
		assertTrue("Process Queued Events should be called", audience.processQueuedEventsCalled);
	}

	// =================================================================================================================
	// void queueAamEvent(final Event event)
	// =================================================================================================================
	@Test
	public void testQueueAamEvent_when_eventIsNull_then_shouldDoNothing() {
		// setup
		final Event testEvent = null;

		// test
		audience.queueAamEvent(testEvent);

		// verify
		assertEquals("Event should not be queued", 0, audience.waitingEvents.size());
		assertFalse("Process Queued Events should not be called", audience.processQueuedEventsCalled);
	}

	@Test
	public void testQueueAamEvent_when_validEvent_then_shouldQueueAndProcessEvents() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("TestAAM", EventType.AUDIENCEMANAGER,
				EventSource.REQUEST_PROFILE).build();

		// test
		audience.queueAamEvent(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals("Event should be queued", 1, audience.waitingEvents.size());
		assertEquals("Event should be correct", testEvent, audience.waitingEvents.peek());
		assertTrue("Process Queued Events should be called", audience.processQueuedEventsCalled);
	}

	// =================================================================================================================
	// void reset(final Event event)
	// =================================================================================================================
	@Test
	public void testReset_when_happy_then_shouldSetStatePropertiesToNullAndUpdateSharedState() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("TestAAM", EventType.AUDIENCEMANAGER,
				EventSource.REQUEST_PROFILE).build();
		final HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		state.setDpid("value");
		state.setDpuuid("value");
		state.setUuid("value");
		getAudienceDataStore().setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);

		// test
		audience.reset(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		//verify
		assertNull(state.getDpid());
		assertNull(state.getDpuuid());
		assertNull(state.getUuid());
		assertNull(state.getVisitorProfile());

		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(testEvent);
		assertEquals(0, aamSharedState.size());
		assertNull("dpid from shared state should be null",
				   aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPID, null));
		assertNull("dpuuid from shared state should be null",
				   aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.DPUUID, null));
		assertNull("uuid from shared state should be null",
				   aamSharedState.optString(AudienceTestConstants.EventDataKeys.Audience.UUID, null));
		assertNull("visitorProfile from shared state should be null",
				   aamSharedState.optStringMap(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE, null));
	}

	@Test
	public void testReset_when_nullEvent_then_shouldReturn() throws Exception {
		// setup
		final Event testEvent = null;
		final String testDpid = "theDpid";
		final String testDpuuid = "theDpuuid";
		final String testUuid = "theUuid";
		final HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		state.setDpid(testDpid);
		state.setDpuuid(testDpuuid);
		state.setUuid(testUuid);
		getAudienceDataStore().setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);

		// test
		audience.reset(testEvent);

		//verify
		assertEquals("dpid should be unchanged", testDpid, state.getDpid());
		assertEquals("dpuuid should be unchanged", testDpuuid, state.getDpuuid());
		assertEquals("uuid should be unchanged", testUuid, state.getUuid());
		assertEquals("visitor profile should be unchanged", visitorProfile, state.getVisitorProfile());

		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("no shared state should be created for this event", aamSharedState);
	}

	// =================================================================================================================
	// void void processConfiguration(Event event)
	// =================================================================================================================
	@Test
	public void testProcessConfiguration_When_OptIn() throws Exception {
		EventData configuration = new EventData();
		configuration.putString("global.privacy", "optedin");
		Event testEvent = new Event.Builder("TEST", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();
		audience.processConfiguration(testEvent);
		waitForExecutor(audience.getExecutor(), 1);
		assertFalse(mockDispatcherAudienceResponseContent.dispatchOptOutResultCalled);
		assertTrue(audience.processQueuedEventsCalled);
		assertTrue(mockAudienceRequestsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(MobilePrivacyStatus.OPT_IN, mockAudienceRequestsDatabase.updatePrivacyStatusParameterPrivacyStatus);
		assertFalse(audience.resetWasCalled);
	}

	@Test
	public void testProcessConfiguration_When_OptOutWithValidUUIDAndAAMServer() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString("global.privacy", "optedout");
		configuration.putString("audience.server", "server");
		Event testEvent = new Event.Builder("TEST", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		audience.waitingEvents.add(aamEvent1);
		audience.waitingEvents.add(aamEvent2);
		audience.internalState.setUuid("testuuid");

		audience.processConfiguration(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		assertTrue(mockDispatcherAudienceResponseContent.dispatchOptOutResultCalled);
		assertTrue(mockDispatcherAudienceResponseContent.dispatchOptOutResultParameterOptedOut);
		assertTrue(audience.processQueuedEventsCalled);
		assertTrue(mockAudienceRequestsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(MobilePrivacyStatus.OPT_OUT, mockAudienceRequestsDatabase.updatePrivacyStatusParameterPrivacyStatus);
		assertTrue(audience.resetWasCalled);
		assertEquals(testEvent, audience.resetParameterEvent);
		assertEquals(0, audience.waitingEvents.size());
	}

	@Test
	public void testProcessConfiguration_When_OptOutWithValidUUIDAndNoAAMServer() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString("global.privacy", "optedout");
		Event testEvent = new Event.Builder("TEST", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		audience.waitingEvents.add(aamEvent1);
		audience.waitingEvents.add(aamEvent2);
		audience.internalState.setUuid("testuuid");

		audience.processConfiguration(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		assertTrue(mockDispatcherAudienceResponseContent.dispatchOptOutResultCalled);
		assertFalse(mockDispatcherAudienceResponseContent.dispatchOptOutResultParameterOptedOut);
		assertTrue(audience.processQueuedEventsCalled);
		assertTrue(mockAudienceRequestsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(MobilePrivacyStatus.OPT_OUT, mockAudienceRequestsDatabase.updatePrivacyStatusParameterPrivacyStatus);
		assertTrue(audience.resetWasCalled);
		assertEquals(testEvent, audience.resetParameterEvent);
		assertEquals(0, audience.waitingEvents.size());
	}

	@Test
	public void testProcessConfiguration_When_OptOutWithNoUUIDAndValidAAMServer() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString("global.privacy", "optedout");
		configuration.putString("audience.server", "server");
		Event testEvent = new Event.Builder("TEST", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		audience.waitingEvents.add(aamEvent1);
		audience.waitingEvents.add(aamEvent2);

		audience.processConfiguration(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		assertTrue(mockDispatcherAudienceResponseContent.dispatchOptOutResultCalled);
		assertFalse(mockDispatcherAudienceResponseContent.dispatchOptOutResultParameterOptedOut);
		assertTrue(audience.processQueuedEventsCalled);
		assertTrue(mockAudienceRequestsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(MobilePrivacyStatus.OPT_OUT, mockAudienceRequestsDatabase.updatePrivacyStatusParameterPrivacyStatus);
		assertTrue(audience.resetWasCalled);
		assertEquals(testEvent, audience.resetParameterEvent);
		assertEquals(0, audience.waitingEvents.size());
	}

	@Test
	public void testProcessConfiguration_When_Unknown() throws Exception {
		EventData configuration = new EventData();
		configuration.putString("global.privacy", "optunknown");
		Event testEvent = new Event.Builder("TEST", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();
		audience.processConfiguration(testEvent);

		waitForExecutor(audience.getExecutor(), 1);

		assertFalse(mockDispatcherAudienceResponseContent.dispatchOptOutResultCalled);
		assertTrue(audience.processQueuedEventsCalled);
		assertTrue(mockAudienceRequestsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(MobilePrivacyStatus.UNKNOWN, mockAudienceRequestsDatabase.updatePrivacyStatusParameterPrivacyStatus);
		assertFalse(audience.resetWasCalled);
	}

	// =================================================================================================================
	// void setDpidAndDpuuid(final String dpid, final String dpuuid)
	// =================================================================================================================
	@Test
	public void testSetDpidAndDpuuid_when_happy_then_shouldUpdateStateAndSharedState() throws Exception {
		// setup
		setAudienceManagerStateProperties();
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String testDpid = "newDpid";
		final String testDpuuid = "newDpuuid";

		// test
		audience.setDpidAndDpuuid(testDpid, testDpuuid, testEvent);
		waitForExecutor(audience.getExecutor(), 1);
		// verify
		assertEquals("state should have correct value for dpid", testDpid, state.getDpid());
		assertEquals("state should have correct value for dpuuid", testDpuuid, state.getDpuuid());

		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(testEvent);
		assertEquals(4, aamSharedState.size());
		assertEquals("dpid from shared state should be updated", testDpid,
					 aamSharedState.getString2(AudienceTestConstants.EventDataKeys.Audience.DPID));
		assertEquals("dpuuid from shared state should be updated", testDpuuid,
					 aamSharedState.getString2(AudienceTestConstants.EventDataKeys.Audience.DPUUID));
		assertEquals("uuid from shared state should be set", "testuuid",
					 aamSharedState.getString2(AudienceTestConstants.EventDataKeys.Audience.UUID));
		assertEquals("visitor profile from shared state should be set", 1,
					 aamSharedState.getStringMap(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE).size());
	}

	@Test
	public void testSetDpidAndDpuuid_when_nullEventParam_then_shouldReturn() throws Exception {
		// setup
		final Event testEvent = null;
		final String testDpid = "newDpid";
		final String testDpuuid = "newDpuuid";

		// test
		audience.setDpidAndDpuuid(testDpid, testDpuuid, testEvent);

		// verify
		assertNotEquals("state should have correct value for dpid", testDpid, state.getDpid());
		assertNotEquals("state should have correct value for dpuuid", testDpuuid, state.getDpuuid());

		final EventData aamSharedState = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("shared state object should not be created for this version", aamSharedState);
	}

	// =================================================================================================================
	// protected void processQueuedEvents()
	// =================================================================================================================
	@Test
	public void testProcessQueuedEvents_when_happy_then_shouldLoopThroughAndSubmitSignalForAllWaitingEvents() throws
		Exception {
		// setup
		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
		mockAudienceExtension.waitingEvents.add(aamEvent1);
		mockAudienceExtension.waitingEvents.add(aamEvent2);
		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		mockAudienceExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be empty", 0, mockAudienceExtension.waitingEvents.size());
		assertEquals("submit signal was called for all 3 events", 3, mockAudienceExtension.submitSignalCallCount);
	}

	@Test
	public void testProcessQueuedEvents_when_noConfiguration_then_shouldNotProcessEvents() throws Exception {
		// setup
		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
		mockAudienceExtension.waitingEvents.add(aamEvent1);
		mockAudienceExtension.waitingEvents.add(aamEvent2);
		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, null);

		// test
		mockAudienceExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be unchanged", 3, mockAudienceExtension.waitingEvents.size());
		assertEquals("submit signal should not be called", 0, mockAudienceExtension.submitSignalCallCount);
	}

	@Test
	public void testProcessQueuedEvents_when_configurationHasNoAamServer_then_shouldNotProcessEvents() throws Exception {
		// setup
		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
		mockAudienceExtension.waitingEvents.add(aamEvent1);
		mockAudienceExtension.waitingEvents.add(aamEvent2);
		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
		final EventData config = getFakeConfigEventData();
		config.putString(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, config);

		// test
		mockAudienceExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be unchanged", 3, mockAudienceExtension.waitingEvents.size());
		assertEquals("submit signal should not be called", 0, mockAudienceExtension.submitSignalCallCount);
	}

	@Test
	public void testProcessQueuedEvents_when_aamForwarding_then_shouldNotProcessLifecycleEvents() throws Exception {
		// setup
		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
		mockAudienceExtension.waitingEvents.add(aamEvent1);
		mockAudienceExtension.waitingEvents.add(aamEvent2);
		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
		final EventData fakeConfigData = getFakeConfigEventData();
		fakeConfigData.putBoolean(AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING, true);
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);

		// test
		mockAudienceExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be empty", 0, mockAudienceExtension.waitingEvents.size());
		assertEquals("submit signal should be called twice", 2, mockAudienceExtension.submitSignalCallCount);
	}

	// =================================================================================================================
	// protected HashMap<String, String> processResponse(final String response, final Event event)
	// =================================================================================================================
	@Test
	public void testProcessResponse_when_happy_then_responseIsProperlyProcessed() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String jsonResponse = "{" +
									"'uuid':'12345', " +
									"'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], " +
									"'dests':[{'c':'https://www.adobe.com'}]}";
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.processResponse(jsonResponse, testEvent);

		// verify
		assertEquals("uuid should be set in state", "12345", audience.internalState.getUuid());
		final HashMap<String, String> profile = (HashMap<String, String>)audience.internalState.getVisitorProfile();
		assertNotNull("visitor profile should be set in state", profile);
		assertEquals("visitor profile should have the correct value", "key1=value1", profile.get("cookieName"));
		assertTrue("the dest was properly forwarded", platformServices.getMockNetworkService().connectUrlAsyncWasCalled);
	}

	@Test
	public void testProcessResponse_when_nullResponse_then_doNothing() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String jsonResponse = null;
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.processResponse(jsonResponse, testEvent);

		// verify
		assertNull("uuid should be null in state", audience.internalState.getUuid());
		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());

		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("shared data object should be null for this event", sharedAamData);
	}

	@Test
	public void testProcessResponse_when_emptyResponse_then_doNothing() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String jsonResponse = "";
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.processResponse(jsonResponse, testEvent);

		// verify
		assertNull("uuid should be null in state", audience.internalState.getUuid());
		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());

		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("shared data object should be null for this event", sharedAamData);
	}

	@Test
	public void testProcessResponse_when_noConfiguration_then_doNothing() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String jsonResponse = "{" +
									"'uuid':'12345', " +
									"'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], " +
									"'dests':[{'c':'https://www.adobe.com'}]}";
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, null);

		// test
		audience.processResponse(jsonResponse, testEvent);

		// verify
		assertNull("uuid should be null in state", audience.internalState.getUuid());
		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());

		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("shared data object should be null for this event", sharedAamData);
	}

	@Test
	public void testProcessResponse_when_malFormedJSON_doNothing() throws Exception {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final String jsonResponse = "{" +
									"'uuid':'12345', " +
									"'stuff':{'cn':'cookieName', 'cv':'key1=value1'}], " +
									"'dests':[{'c':'https://www.adobe.com']";
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.processResponse(jsonResponse, testEvent);

		// verify
		assertNull("uuid should be null in state", audience.internalState.getUuid());
		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());

		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
		assertNull("shared data object should be null for this event", sharedAamData);
	}

	// =================================================================================================================
	// protected void submitSignal(final Event event)
	// =================================================================================================================
	@Test
	public void testSubmitSignal_when_AudienceNotConfigured() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		// test
		audience.submitSignal(event);

		// verify
		assertFalse(mockDispatcherAudienceResponseContent.dispatchWasCalled);
	}

	@Test
	public void testSubmitSignal_when_MarketingCloudOrgIDSet() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		final EventData fakeConfigData = getFakeConfigEventData();
		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
								 "testExperience@adobeorg");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);

		// test
		audience.submitSignal(event);

		// verify
		assertTrue(mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_orgid=testExperience@adobeorg"));
	}

	@Test
	public void testSubmitSignal_when_PrivacyOptOut() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		final EventData fakeConfigData = getFakeConfigEventData();
		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);

		// test
		audience.submitSignal(event);

		// verify
		assertTrue(mockDispatcherAudienceResponseContent.dispatchWasCalled);
		assertNull(mockDispatcherAudienceResponseContent.dispatchParametersProfileMap);
	}

	@Test
	public void testSubmitSignal_when_PrivacyUnknown() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		final EventData fakeConfigData = getFakeConfigEventData();
		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request not got queued", mockAudienceRequestsDatabase.queueWasCalled);
	}

	@Test
	public void testSubmitSignal_when_serverEmptyAndPairIdIsNull() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
		final EventData configData = getFakeConfigEventData();
		configData.putString(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, configData);

		// test
		audience.submitSignal(event);

		// verify
		assertTrue(mockDispatcherAudienceResponseContent.dispatchWasCalled);
	}

	@Test
	public void testSubmitSignal_when_systemInfoService_doesnotHavePlatformInformation() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		platformServices.getMockSystemInfoService().mockCanonicalPlatformName = null;

		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());

		setAudienceManagerStateProperties();

		// test
		audience.submitSignal(event);

		// verify
		assertTrue(mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=java"));
	}

	@Test
	public void testSubmitSignal_happyPath() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());

		setAudienceManagerStateProperties();

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains(
					   "d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"));
		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	}

	@Test
	public void testSubmitSignal_happyPath_noCustomData() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(null, "pairId");

		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());

		setAudienceManagerStateProperties();

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains(
					   "d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"));
		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	}

	@Test
	public void testSubmitSignal_happyPath_traitWithNullValue() throws Exception {
		// setup
		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
		additionalTraits.put("nullKey", null);
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");

		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());

		setAudienceManagerStateProperties();

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_nullKey="));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains(
					   "d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"));
		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	}

	@Test
	public void testSubmitSignal_NullIdentitySharedState() throws Exception {
		// setup
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, null);

		setAudienceManagerStateProperties();

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains(
						"d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"));
		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	}

	@Test
	public void testSubmitSignal_SanitizesTraitKeys() throws Exception {
		// setup
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
		additionalTraits.put("trait.key", "trait.value");
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_trait_key=trait.value"));
	}

	@Test
	public void testSubmitSignal_Trait_with_EmptyKey() throws Exception {
		// setup
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
		additionalTraits.put("", "traitvalue");
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_=traitvalue"));
	}

	@Test
	public void testSubmitSignal_when_dpid_notProvided() throws Exception {
		// setup
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		state.setDpuuid("testdpuuid");

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_uuid=testuuid"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_dpid=testdpid"));
	}

	@Test
	public void testSubmitSignal_when_dpuuid_notProvided() throws Exception {
		// setup
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		state.setDpid("testdpid");

		// test
		audience.submitSignal(event);

		// verify
		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_uuid=testuuid"));
		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_dpid=testdpid"));
	}

	// =================================================================================================================
	// protected HashMap<String, String> handleNetworkResponse(final String response, final Event event)
	// =================================================================================================================
	@Test
	public void testHandleNetworkResponse_when_response_isEmpty() throws Exception {
		// setup
		final String mockResponse = "";
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
		assertTrue(mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.isEmpty());
	}

	@Test
	public void testHandleNetworkResponse_when_response_hasNoProfile() throws Exception {
		// setup
		final String mockResponse = "{'uuid':'testuuid'}";
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals("testuuid", state.getUuid());
		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
	}

	@Test
	public void testHandleNetworkResponse_when_response_EmptyStuffArray() throws Exception {
		// setup
		final String mockResponse = "{'stuff':[]}";
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
	}

	@Test
	public void testHandleNetworkResponse_when_response_ValidStuffArray() throws Exception {
		// setup
		final String stuffArrayAsString = prepareStuffArray().toString();
		final String mockResponse = "{'stuff':" + stuffArrayAsString + "}";
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(2, mockDispatcherAudienceResponseContent.dispatchCallCount);
		assertEquals(2, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
		assertEquals(2, state.getVisitorProfile().size());
		assertEquals("seg=mobile_android", mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.get("aud"));
		assertEquals("seg=mobile_android", state.getVisitorProfile().get("aud"));
		assertEquals("cookieValue", mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.get("cookieKey"));
		assertEquals("cookieValue", state.getVisitorProfile().get("cookieKey"));
	}

	@Test
	public void testHandleNetworkResponse_when_response_ValidDestArray() throws Exception {
		// setup
		final String destsArrayAsString = prepareDestArray().toString();
		final String mockResponse = "{'dests':" + destsArrayAsString + "}";
		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
		assertEquals("desturl", mockNetworkService.connectUrlAsyncParametersUrl);
		assertEquals(HttpCommand.GET, mockNetworkService.connectUrlAsyncParametersCommand);
	}

	@Test
	public void testHandleNetworkResponse_when_response_ValidDestArray_responsePairID_NotAvailable_ForEvent() throws
		Exception {
		// setup
		final String destsArrayAsString = prepareDestArray().toString();
		final String mockResponse = "{'dests':" + destsArrayAsString + "}";
		final Event event = getSubmitSignalEventWithOutPairID(getFakeAamTraitsEventData(), "pairId");
		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, getFakeConfigEventData());

		// test
		audience.handleNetworkResponse(mockResponse, event);
		waitForExecutor(audience.getExecutor(), 1);

		// verify
		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchCallCount);
		assertEquals("desturl", mockNetworkService.connectUrlAsyncParametersUrl);
		assertEquals(HttpCommand.GET, mockNetworkService.connectUrlAsyncParametersCommand);
	}

	// =================================================================================================================
	// helper methods
	// =================================================================================================================
	private LocalStorageService.DataStore getAudienceDataStore() {
		return fakeLocalStorageService.getDataStore(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_DATA_STORE);
	}

	private EventData getFakeAamTraitsEventData() {
		return getFakeAamTraitsEventData(null);
	}

	private EventData getFakeAamTraitsEventData(final Map<String, String> additionalTraits) {
		final EventData eventData = new EventData();
		eventData.putStringMap(AudienceTestConstants.EventDataKeys.Audience.VISITOR_TRAITS, traits(additionalTraits));

		return eventData;
	}

	private EventData getFakeConfigEventData() {
		final EventData fakeConfigData = new EventData();
		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "server");
		fakeConfigData.putInteger(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT, 4);
		fakeConfigData.putBoolean(AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING, false);

		return fakeConfigData;
	}

	private EventData getFakeIdentityEventData() {
		final EventData fakeIdentityData = new EventData();
		fakeIdentityData.putString(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testMarketingID");
		fakeIdentityData.putString(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_BLOB, "testBlob");
		fakeIdentityData.putString(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_LOCATION_HINT, "testLocationHint");

		List<VisitorID> visitorIDList = new ArrayList<VisitorID>();
		visitorIDList.add(new VisitorID("d_cid_ic", "id_type1", "id1", VisitorID.AuthenticationState.AUTHENTICATED));
		visitorIDList.add(new VisitorID("d_cid_ic", "id_type2", "id2", VisitorID.AuthenticationState.LOGGED_OUT));
		visitorIDList.add(new VisitorID("d_cid_ic", "id_type3", null, VisitorID.AuthenticationState.LOGGED_OUT));
		visitorIDList.add(new VisitorID("d_cid_ic", "id_type4", "id4", VisitorID.AuthenticationState.UNKNOWN));
		fakeIdentityData.putTypedList(AudienceTestConstants.EventDataKeys.Identity.VISITOR_IDS_LIST, visitorIDList,
									  VisitorID.VARIANT_SERIALIZER);

		return fakeIdentityData;
	}

	private EventData getFakeLifecycleEventData() {
		final EventData fakeLifecycleData = new EventData();
		fakeLifecycleData.putStringMap(AudienceTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA,
									   fakeLifeCycleData());
		return fakeLifecycleData;
	}

	private Event getSubmitSignalEvent(final EventData eventData, final String pairId) {
		return new Event.Builder("TEST", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			   .setPairID(pairId).setData(eventData).build();
	}

	private Event getSubmitSignalEventWithOutPairID(final EventData eventData, final String pairId) {
		return new Event.Builder("TEST", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			   .setPairID(pairId).setData(eventData).setResponsePairID(null).build();
	}

	private Event getLifecycleEvent(final EventData eventData, final String pairId) {
		return new Event.Builder("TEST", EventType.LIFECYCLE, EventSource.REQUEST_CONTENT)
			   .setPairID(pairId).setData(eventData).build();
	}

	private void setAudienceManagerStateProperties() {
		state.setDpid("testdpid");
		state.setDpuuid("testdpuuid");
		state.setUuid("testuuid");
		HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		getAudienceDataStore().setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);
	}

	private HashMap<String, String> traits(final Map<String, String> additionalTraits) {
		HashMap<String, String> traits = new HashMap<String, String>();
		traits.put("traitKey", "traitValue");

		if (additionalTraits != null) {
			traits.putAll(additionalTraits);
		}

		return traits;
	}

	private JSONArray prepareStuffArray() throws JsonException {
		JSONArray stuffArray = fakeJsonUtilityService.createJSONArray("[]");
		JSONObject stuffValid1 = fakeJsonUtilityService.createJSONObject("{}");
		stuffValid1.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "aud");
		stuffValid1.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "seg=mobile_android");
		stuffValid1.put("ttl", 0);
		stuffValid1.put("dmn", "audidute.com");
		JSONObject stuffValid2 = fakeJsonUtilityService.createJSONObject("{}");
		stuffValid2.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "cookieKey");
		stuffValid2.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "cookieValue");
		stuffArray.put(stuffValid1);
		stuffArray.put(stuffValid2);
		return stuffArray;
	}

	private JSONArray prepareDestArray() throws JsonException {
		JSONArray destArray = fakeJsonUtilityService.createJSONArray("[]");
		JSONObject destURL = fakeJsonUtilityService.createJSONObject("{}");
		destURL.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_URL_KEY, "desturl");
		destArray.put(destURL);
		return destArray;
	}

	private HashMap<String, String> fakeLifeCycleData() {
		HashMap<String, String> lifecycleData = new HashMap<String, String>();
		lifecycleData.put("appid", "someAppID");
		lifecycleData.put("launches", "2");
		return lifecycleData;
	}

}
