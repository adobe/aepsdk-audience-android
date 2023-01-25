/*
  Copyright 2017 Adobe. All rights reserved.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//
//import com.adobe.marketing.mobile.JsonUtilityService.*;
//import com.adobe.marketing.mobile.NetworkService.HttpCommand;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.DataQueuing;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AudienceExtensionTest {

	//
	//	private MockNetworkService mockNetworkService;
	//	private LocalStorageService fakeLocalStorageService;
	//	private JsonUtilityService fakeJsonUtilityService;
	//	private MockDispatcherAudienceResponseContentAudienceManager mockDispatcherAudienceResponseContent;
	//	private MockDispatcherAudienceResponseIdentityAudienceManager mockDispatcherAudienceResponseIdentity;
	//	private MockAudienceRequestsDatabase mockAudienceRequestsDatabase;
	private AudienceExtension audience;

	@Mock
	private AudienceState mockState;

	@Mock
	private ExtensionApi mockExtensionApi;

	@Mock
	private AudienceHitProcessor mockHitProcessor;

	@Mock
	private DataQueuing mockDataQueueService;

	@Mock
	private DataQueue mockDataQueue;

	@Mock
	private Networking mockNetworkService;

	@Mock
	private ServiceProvider mockServiceProvider;

	private MockedStatic<ServiceProvider> mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider.class);

	@Before
	public void setup() {
		mockedStaticServiceProvider.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getDataQueueService()).thenReturn(mockDataQueueService);
		when(mockDataQueueService.getDataQueue("com.adobe.module.audience")).thenReturn(mockDataQueue);
		when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
		audience = new AudienceExtension(mockExtensionApi, mockState, mockHitProcessor);
	}

	//
	//	@Before
	//	public void setup() throws MissingPlatformServicesException {
	//		super.beforeEach();
	//		// Mock Platform Services
	//		mockNetworkService = platformServices.getMockNetworkService();
	//		fakeJsonUtilityService = platformServices.getJsonUtilityService();
	//		fakeLocalStorageService = platformServices.getLocalStorageService();
	//		mockDispatcherAudienceResponseContent = new MockDispatcherAudienceResponseContentAudienceManager();
	//		mockDispatcherAudienceResponseIdentity = new MockDispatcherAudienceResponseIdentityAudienceManager();
	//		state = new AudienceState(fakeLocalStorageService);
	//		state.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
	//		audience =
	//			new TestableAudience(
	//				eventHub,
	//				platformServices,
	//				mockDispatcherAudienceResponseContent,
	//				mockDispatcherAudienceResponseIdentity,
	//				state
	//			);
	//		mockAudienceRequestsDatabase = (MockAudienceRequestsDatabase) audience.internalDatabase;
	//	}
	//
	//	// =================================================================================================================
	//	// void bootup(final Event bootEvent)
	//	// =================================================================================================================
	@Test
	public void testOnRegistered_sharesAudienceSharedState() {
		// setup
		final HashMap<String, String> visitorProfile = new HashMap<String, String>();
		visitorProfile.put("someKey", "someValue");
		when(mockState.getUuid()).thenReturn("mock-uuid");
		when(mockState.getVisitorProfile()).thenReturn(visitorProfile);
		when(mockState.getStateData()).thenCallRealMethod(); // allows calls to the mocked getters

		// test
		audience.onRegistered();

		// verify
		ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockExtensionApi).createSharedState(sharedStateCaptor.capture(), isNull());
		Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
		assertNotNull(aamSharedState);
		assertEquals(2, aamSharedState.size());
		assertEquals(
			"mock-uuid",
			DataReader.optString(aamSharedState, AudienceTestConstants.EventDataKeys.Audience.UUID, "")
		);
		assertEquals(
			visitorProfile,
			DataReader.optStringMap(aamSharedState, AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE, null)
		);
	}

	@Test
	public void testOnRegistered_whenUUIDNotAvailable_sharesEmptyAudienceSharedState() {
		// setup
		when(mockState.getUuid()).thenReturn(null);
		when(mockState.getVisitorProfile()).thenReturn(null);
		when(mockState.getStateData()).thenCallRealMethod(); // allows calls to the mocked getters

		// test
		audience.onRegistered();

		// verify
		ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockExtensionApi).createSharedState(sharedStateCaptor.capture(), isNull());
		Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
		assertNotNull(aamSharedState);
		assertEquals(0, aamSharedState.size());
	}

	@Test
	public void testOnRegistered_registersCorrectListeners() {
		audience.onRegistered();

		// verify
		final ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> eventSourceCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<ExtensionEventListener> listenerCaptor = ArgumentCaptor.forClass(
			ExtensionEventListener.class
		);
		verify(mockExtensionApi, times(7))
			.registerEventListener(eventTypeCaptor.capture(), eventSourceCaptor.capture(), listenerCaptor.capture());
		assertEquals(EventType.ANALYTICS, eventTypeCaptor.getAllValues().get(0));
		assertEquals(EventSource.RESPONSE_CONTENT, eventSourceCaptor.getAllValues().get(0));
		assertEquals(EventType.AUDIENCEMANAGER, eventTypeCaptor.getAllValues().get(1));
		assertEquals(EventSource.REQUEST_CONTENT, eventSourceCaptor.getAllValues().get(1));
		assertEquals(EventType.AUDIENCEMANAGER, eventTypeCaptor.getAllValues().get(2));
		assertEquals(EventSource.REQUEST_IDENTITY, eventSourceCaptor.getAllValues().get(2));
		assertEquals(EventType.AUDIENCEMANAGER, eventTypeCaptor.getAllValues().get(3));
		assertEquals(EventSource.REQUEST_RESET, eventSourceCaptor.getAllValues().get(3));
		assertEquals(EventType.CONFIGURATION, eventTypeCaptor.getAllValues().get(4));
		assertEquals(EventSource.RESPONSE_CONTENT, eventSourceCaptor.getAllValues().get(4));
		assertEquals(EventType.GENERIC_IDENTITY, eventTypeCaptor.getAllValues().get(5));
		assertEquals(EventSource.REQUEST_RESET, eventSourceCaptor.getAllValues().get(5));
		assertEquals(EventType.LIFECYCLE, eventTypeCaptor.getAllValues().get(6));
		assertEquals(EventSource.RESPONSE_CONTENT, eventSourceCaptor.getAllValues().get(6));
	}

	@Test
	public void testGetName() {
		assertEquals("com.adobe.module.audience", audience.getName());
	}

	@Test
	public void testGetFriendlyName() {
		assertEquals("Audience", audience.getFriendlyName());
	}

	@Test
	public void testGetVersion_notNull() {
		assertNotNull(audience.getVersion());
	}

	@Test
	public void testHandleAudienceRequestIdentity_dispatchesResponseEventWithVisitorProfile() {
		// setup
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
		HashMap<String, String> visitorProfile = new HashMap<>();
		visitorProfile.put("someKey", "someValue");
		when(mockState.getVisitorProfile()).thenReturn(visitorProfile);

		// test
		audience.handleAudienceRequestIdentity(testEvent);

		// verify
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		final Event responseEvent = eventCaptor.getValue();
		assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
		assertEquals(EventSource.RESPONSE_IDENTITY, responseEvent.getSource());
		assertEquals(testEvent.getUniqueIdentifier(), responseEvent.getResponseID()); // verifies in response to request event
		assertEquals(1, responseEvent.getEventData().size());
		assertEquals(
			visitorProfile,
			DataReader.optStringMap(
				responseEvent.getEventData(),
				AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
				null
			)
		);
	}

	@Test
	public void testHandleAudienceRequestIdentity_whenNoVisitorProfile_dispatchesResponseEvent()
		throws DataReaderException {
		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
		when(mockState.getVisitorProfile()).thenReturn(null);

		// test
		audience.handleAudienceRequestIdentity(testEvent);

		// verify
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		final Event responseEvent = eventCaptor.getValue();
		assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
		assertEquals(EventSource.RESPONSE_IDENTITY, responseEvent.getSource());
		assertEquals(testEvent.getUniqueIdentifier(), responseEvent.getResponseID()); // verifies in response to request event
		assertEquals(1, responseEvent.getEventData().size());
		assertTrue(
			responseEvent.getEventData().containsKey(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE)
		);
		assertNull(
			DataReader.getStringMap(
				responseEvent.getEventData(),
				AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE
			)
		);
	}

	@Test
	public void testHandleResetIdentities_whenAudienceReset_resetsInternalStateAndClearsSharedState() {
		// setup
		final Event testEvent = new Event.Builder("TestAAMReset", EventType.AUDIENCEMANAGER, EventSource.REQUEST_RESET)
			.build();

		// test
		audience.handleResetIdentities(testEvent);

		verify(mockState).clearIdentifiers();
		verify(mockState).setLastResetTimestamp(eq(testEvent.getTimestamp()));
		verifyNoInteractions(mockDataQueue);

		//verify
		ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi).createSharedState(sharedStateCaptor.capture(), eventCaptor.capture());
		Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
		assertNotNull(aamSharedState);
		assertEquals(0, aamSharedState.size());
		assertEquals(testEvent.getUniqueIdentifier(), eventCaptor.getValue().getUniqueIdentifier());
	}

	@Test
	public void testHandleResetIdentities_whenGenericIdentityReset_resetsInternalStateAndClearsSharedStateAndQueue() {
		// setup
		final Event testEvent = new Event.Builder(
			"TestGenericReset",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_RESET
		)
			.build();

		// test
		audience.handleResetIdentities(testEvent);

		verify(mockState).clearIdentifiers();
		verify(mockState).setLastResetTimestamp(eq(testEvent.getTimestamp()));
		verifyNoInteractions(mockDataQueue);

		//verify
		ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi).createSharedState(sharedStateCaptor.capture(), eventCaptor.capture());
		Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
		assertNotNull(aamSharedState);
		assertEquals(0, aamSharedState.size());
		assertEquals(testEvent.getUniqueIdentifier(), eventCaptor.getValue().getUniqueIdentifier());
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyOptedIn_updatesInternalState() {
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optedin");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		//
		verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_IN));
		// todo: verify(mockDataQueue).handlePrivacyChange
		verify(mockExtensionApi, never()).dispatch(any(Event.class));
		verify(mockExtensionApi).createSharedState(any(), any(Event.class));
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyUnknown_updatesInternalState() {
		when(mockState.getUuid()).thenReturn("testuuid");
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optunknown");
		configuration.put("audience.server", "server.com");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		//
		verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.UNKNOWN));
		// todo: verify(mockDataQueue).handlePrivacyChange
		verify(mockExtensionApi, never()).dispatch(any(Event.class));
		verify(mockExtensionApi).createSharedState(any(), any(Event.class));
		verifyNoInteractions(mockNetworkService);
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyOptedOutNoUUIDAndNoAAMServer_updatesInternalStateAndDispatchesOptoutEvent() {
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optedout");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		// verify
		verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_OUT));
		// todo: verify(mockDataQueue).handlePrivacyChange
		verifyNoInteractions(mockNetworkService);
		verify(mockExtensionApi).createSharedState(any(), any(Event.class));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		assertOptOutEvent(eventCaptor.getValue(), false);
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyOptedOutAndAAMServer_sendsOptoutNetworkRequestAndDispatchesEvent() {
		// setup
		when(mockState.getUuid()).thenReturn("testuuid");
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optedout");
		configuration.put("audience.server", "server.com");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		// verify
		verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_OUT));
		// todo: verify(mockDataQueue).handlePrivacyChange
		ArgumentCaptor<NetworkRequest> networkRequestCaptor = ArgumentCaptor.forClass(NetworkRequest.class);
		verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), notNull());
		assertEquals("https://server.com/demoptout.jpg?d_uuid=testuuid", networkRequestCaptor.getValue().getUrl());
		assertEquals(HttpMethod.GET, networkRequestCaptor.getValue().getMethod());

		verify(mockExtensionApi).createSharedState(any(), any(Event.class));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		assertOptOutEvent(eventCaptor.getValue(), true);
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyOptedOutValidUUIDAndNoAAMServer_noNetworkRequestAndDispatchesOptoutEvent() {
		// setup
		when(mockState.getUuid()).thenReturn("testuuid");
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optedout");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		// verify
		verifyNoInteractions(mockNetworkService);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		assertOptOutEvent(eventCaptor.getValue(), false);
	}

	@Test
	public void testHandleConfigurationResponse_whenPrivacyOptedOutNoUUIDAndValidAAMServer_noNetworkRequestAndDispatchesOptoutEvent() {
		// setup
		when(mockState.getUuid()).thenReturn(null);
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("global.privacy", "optedout");
		configuration.put("audience.server", "server.com");
		Event testEvent = new Event.Builder("TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configuration)
			.build();

		// test
		audience.handleConfigurationResponse(testEvent);

		// verify
		verifyNoInteractions(mockNetworkService);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
		assertOptOutEvent(eventCaptor.getValue(), false);
	}

	//	// =================================================================================================================
	//	// protected void processQueuedEvents()
	//	// =================================================================================================================
	//	@Test
	//	public void testProcessQueuedEvents_when_happy_then_shouldLoopThroughAndSubmitSignalForAllWaitingEvents()
	//		throws Exception {
	//		// setup
	//		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
	//		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
	//		mockAudienceExtension.waitingEvents.add(aamEvent1);
	//		mockAudienceExtension.waitingEvents.add(aamEvent2);
	//		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		mockAudienceExtension.processQueuedEvents();
	//
	//		// verify
	//		assertEquals("waiting events queue should be empty", 0, mockAudienceExtension.waitingEvents.size());
	//		assertEquals("submit signal was called for all 3 events", 3, mockAudienceExtension.submitSignalCallCount);
	//	}
	//
	//	@Test
	//	public void testProcessQueuedEvents_when_noConfiguration_then_shouldNotProcessEvents() throws Exception {
	//		// setup
	//		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
	//		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
	//		mockAudienceExtension.waitingEvents.add(aamEvent1);
	//		mockAudienceExtension.waitingEvents.add(aamEvent2);
	//		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, null);
	//
	//		// test
	//		mockAudienceExtension.processQueuedEvents();
	//
	//		// verify
	//		assertEquals("waiting events queue should be unchanged", 3, mockAudienceExtension.waitingEvents.size());
	//		assertEquals("submit signal should not be called", 0, mockAudienceExtension.submitSignalCallCount);
	//	}
	//
	//	@Test
	//	public void testProcessQueuedEvents_when_configurationHasNoAamServer_then_shouldNotProcessEvents()
	//		throws Exception {
	//		// setup
	//		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
	//		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
	//		mockAudienceExtension.waitingEvents.add(aamEvent1);
	//		mockAudienceExtension.waitingEvents.add(aamEvent2);
	//		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
	//		final EventData config = getFakeConfigEventData();
	//		config.putString(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "");
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, config);
	//
	//		// test
	//		mockAudienceExtension.processQueuedEvents();
	//
	//		// verify
	//		assertEquals("waiting events queue should be unchanged", 3, mockAudienceExtension.waitingEvents.size());
	//		assertEquals("submit signal should not be called", 0, mockAudienceExtension.submitSignalCallCount);
	//	}
	//
	//	@Test
	//	public void testProcessQueuedEvents_when_aamForwarding_then_shouldNotProcessLifecycleEvents() throws Exception {
	//		// setup
	//		final MockAudienceExtension mockAudienceExtension = new MockAudienceExtension(eventHub, platformServices);
	//		final Event aamEvent1 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event aamEvent2 = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData(), null);
	//		mockAudienceExtension.waitingEvents.add(aamEvent1);
	//		mockAudienceExtension.waitingEvents.add(aamEvent2);
	//		mockAudienceExtension.waitingEvents.add(lifecycleEvent);
	//		final EventData fakeConfigData = getFakeConfigEventData();
	//		fakeConfigData.putBoolean(
	//			AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
	//			true
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);
	//
	//		// test
	//		mockAudienceExtension.processQueuedEvents();
	//
	//		// verify
	//		assertEquals("waiting events queue should be empty", 0, mockAudienceExtension.waitingEvents.size());
	//		assertEquals("submit signal should be called twice", 2, mockAudienceExtension.submitSignalCallCount);
	//	}
	//
	//	// =================================================================================================================
	//	// protected HashMap<String, String> processResponse(final String response, final Event event)
	//	// =================================================================================================================
	//	@Test
	//	public void testProcessResponse_when_happy_then_responseIsProperlyProcessed() throws Exception {
	//		// setup
	//		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final String jsonResponse =
	//			"{" +
	//			"'uuid':'12345', " +
	//			"'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], " +
	//			"'dests':[{'c':'https://www.adobe.com'}]}";
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.processResponse(jsonResponse, testEvent);
	//
	//		// verify
	//		assertEquals("uuid should be set in state", "12345", audience.internalState.getUuid());
	//		final HashMap<String, String> profile = (HashMap<String, String>) audience.internalState.getVisitorProfile();
	//		assertNotNull("visitor profile should be set in state", profile);
	//		assertEquals("visitor profile should have the correct value", "key1=value1", profile.get("cookieName"));
	//		assertTrue(
	//			"the dest was properly forwarded",
	//			platformServices.getMockNetworkService().connectUrlAsyncWasCalled
	//		);
	//	}
	//
	//	@Test
	//	public void testProcessResponse_when_nullResponse_then_doNothing() throws Exception {
	//		// setup
	//		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final String jsonResponse = null;
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.processResponse(jsonResponse, testEvent);
	//
	//		// verify
	//		assertNull("uuid should be null in state", audience.internalState.getUuid());
	//		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());
	//
	//		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
	//		assertNull("shared data object should be null for this event", sharedAamData);
	//	}
	//
	//	@Test
	//	public void testProcessResponse_when_emptyResponse_then_doNothing() throws Exception {
	//		// setup
	//		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final String jsonResponse = "";
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.processResponse(jsonResponse, testEvent);
	//
	//		// verify
	//		assertNull("uuid should be null in state", audience.internalState.getUuid());
	//		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());
	//
	//		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
	//		assertNull("shared data object should be null for this event", sharedAamData);
	//	}
	//
	//	@Test
	//	public void testProcessResponse_when_noConfiguration_then_doNothing() throws Exception {
	//		// setup
	//		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final String jsonResponse =
	//			"{" +
	//			"'uuid':'12345', " +
	//			"'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], " +
	//			"'dests':[{'c':'https://www.adobe.com'}]}";
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, null);
	//
	//		// test
	//		audience.processResponse(jsonResponse, testEvent);
	//
	//		// verify
	//		assertNull("uuid should be null in state", audience.internalState.getUuid());
	//		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());
	//
	//		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
	//		assertNull("shared data object should be null for this event", sharedAamData);
	//	}
	//
	//	@Test
	//	public void testProcessResponse_when_malFormedJSON_doNothing() throws Exception {
	//		// setup
	//		final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final String jsonResponse =
	//			"{" +
	//			"'uuid':'12345', " +
	//			"'stuff':{'cn':'cookieName', 'cv':'key1=value1'}], " +
	//			"'dests':[{'c':'https://www.adobe.com']";
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.processResponse(jsonResponse, testEvent);
	//
	//		// verify
	//		assertNull("uuid should be null in state", audience.internalState.getUuid());
	//		assertNull("visitor profile should be null in state", audience.internalState.getVisitorProfile());
	//
	//		final EventData sharedAamData = audience.getSharedAAMStateFromEventHub(testEvent);
	//		assertNull("shared data object should be null for this event", sharedAamData);
	//	}
	//
	//	// =================================================================================================================
	//	// protected void submitSignal(final Event event) // handleAudienceRequestContent
	//	// =================================================================================================================
	//	@Test
	//	public void testSubmitSignal_when_AudienceNotConfigured() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertFalse(mockDispatcherAudienceResponseContent.dispatchWasCalled);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_MarketingCloudOrgIDSet() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		final EventData fakeConfigData = getFakeConfigEventData();
	//		fakeConfigData.putString(
	//			AudienceTestConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
	//			"testExperience@adobeorg"
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue(mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_orgid=testExperience@adobeorg"));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_PrivacyOptOut() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		final EventData fakeConfigData = getFakeConfigEventData();
	//		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue(mockDispatcherAudienceResponseContent.dispatchWasCalled);
	//		assertNull(mockDispatcherAudienceResponseContent.dispatchParametersProfileMap);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_PrivacyUnknown() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		final EventData fakeConfigData = getFakeConfigEventData();
	//		fakeConfigData.putString(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, fakeConfigData);
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request not got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_serverEmptyAndPairIdIsNull() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), null);
	//		final EventData configData = getFakeConfigEventData();
	//		configData.putString(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "");
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME, configData);
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue(mockDispatcherAudienceResponseContent.dispatchWasCalled);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_systemInfoService_doesnotHavePlatformInformation() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		platformServices.getMockSystemInfoService().mockCanonicalPlatformName = null;
	//
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());
	//
	//		setAudienceManagerStateProperties();
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue(mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=java"));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_happyPath() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());
	//
	//		setAudienceManagerStateProperties();
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
	//		assertTrue(
	//			mockAudienceRequestsDatabase.queueParameterUrl.contains(
	//				"d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"
	//			)
	//		);
	//		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_happyPath_noCustomData() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(null, "pairId");
	//
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());
	//
	//		setAudienceManagerStateProperties();
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
	//		assertTrue(
	//			mockAudienceRequestsDatabase.queueParameterUrl.contains(
	//				"d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"
	//			)
	//		);
	//		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_happyPath_traitWithNullValue() throws Exception {
	//		// setup
	//		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
	//		additionalTraits.put("nullKey", null);
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");
	//
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, getFakeIdentityEventData());
	//
	//		setAudienceManagerStateProperties();
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_nullKey="));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
	//		assertTrue(
	//			mockAudienceRequestsDatabase.queueParameterUrl.contains(
	//				"d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"
	//			)
	//		);
	//		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_NullIdentitySharedState() throws Exception {
	//		// setup
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		eventHub.setSharedState(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME, null);
	//
	//		setAudienceManagerStateProperties();
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_traitKey=traitValue"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_uuid=testuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpid=testdpid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dpuuid=testdpuuid"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_ptfm=mockPlatform"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_dst=1"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_rtbd=json"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_mid=testMarketingID"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("d_blob=testBlob"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("dcs_region=testLocationHint"));
	//		assertFalse(
	//			mockAudienceRequestsDatabase.queueParameterUrl.contains(
	//				"d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"
	//			)
	//		);
	//		assertEquals(4, mockAudienceRequestsDatabase.queueParameterTimeout);
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_SanitizesTraitKeys() throws Exception {
	//		// setup
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
	//		additionalTraits.put("trait.key", "trait.value");
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_trait_key=trait.value"));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_Trait_with_EmptyKey() throws Exception {
	//		// setup
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		final HashMap<String, String> additionalTraits = new HashMap<String, String>();
	//		additionalTraits.put("", "traitvalue");
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits), "pairId");
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("c_=traitvalue"));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_dpid_notProvided() throws Exception {
	//		// setup
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		state.setDpuuid("testdpuuid");
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_uuid=testuuid"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_dpid=testdpid"));
	//	}
	//
	//	@Test
	//	public void testSubmitSignal_when_dpuuid_notProvided() throws Exception {
	//		// setup
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		state.setDpid("testdpid");
	//
	//		// test
	//		audience.submitSignal(event);
	//
	//		// verify
	//		assertTrue("request got queued", mockAudienceRequestsDatabase.queueWasCalled);
	//		assertTrue(mockAudienceRequestsDatabase.queueParameterUrl.startsWith("https://server/event?"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_uuid=testuuid"));
	//		assertFalse(mockAudienceRequestsDatabase.queueParameterUrl.contains("&d_dpid=testdpid"));
	//	}
	//
	//	// =================================================================================================================
	//	// protected HashMap<String, String> handleNetworkResponse(final String response, final Event event)
	//	// =================================================================================================================
	//	@Test
	//	public void testHandleNetworkResponse_when_response_isEmpty() throws Exception {
	//		// setup
	//		final String mockResponse = "";
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//		assertTrue(mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.isEmpty());
	//	}
	//
	//	@Test
	//	public void testHandleNetworkResponse_when_response_hasNoProfile() throws Exception {
	//		// setup
	//		final String mockResponse = "{'uuid':'testuuid'}";
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals("testuuid", state.getUuid());
	//		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//	}
	//
	//	@Test
	//	public void testHandleNetworkResponse_when_response_EmptyStuffArray() throws Exception {
	//		// setup
	//		final String mockResponse = "{'stuff':[]}";
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
	//	}
	//
	//	@Test
	//	public void testHandleNetworkResponse_when_response_ValidStuffArray() throws Exception {
	//		// setup
	//		final String stuffArrayAsString = prepareStuffArray().toString();
	//		final String mockResponse = "{'stuff':" + stuffArrayAsString + "}";
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals(2, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//		assertEquals(2, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
	//		assertEquals(2, state.getVisitorProfile().size());
	//		assertEquals(
	//			"seg=mobile_android",
	//			mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.get("aud")
	//		);
	//		assertEquals("seg=mobile_android", state.getVisitorProfile().get("aud"));
	//		assertEquals(
	//			"cookieValue",
	//			mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.get("cookieKey")
	//		);
	//		assertEquals("cookieValue", state.getVisitorProfile().get("cookieKey"));
	//	}
	//
	//	@Test
	//	public void testHandleNetworkResponse_when_response_ValidDestArray() throws Exception {
	//		// setup
	//		final String destsArrayAsString = prepareDestArray().toString();
	//		final String mockResponse = "{'dests':" + destsArrayAsString + "}";
	//		final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(), "pairId");
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals(1, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchParametersProfileMap.size());
	//		assertEquals("desturl", mockNetworkService.connectUrlAsyncParametersUrl);
	//		assertEquals(HttpCommand.GET, mockNetworkService.connectUrlAsyncParametersCommand);
	//	}
	//
	//	@Test
	//	public void testHandleNetworkResponse_when_response_ValidDestArray_responsePairID_NotAvailable_ForEvent()
	//		throws Exception {
	//		// setup
	//		final String destsArrayAsString = prepareDestArray().toString();
	//		final String mockResponse = "{'dests':" + destsArrayAsString + "}";
	//		final Event event = getSubmitSignalEventWithOutPairID(getFakeAamTraitsEventData(), "pairId");
	//		eventHub.setSharedState(
	//			AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME,
	//			getFakeConfigEventData()
	//		);
	//
	//		// test
	//		audience.handleNetworkResponse(mockResponse, event);
	//		waitForExecutor(audience.getExecutor(), 1);
	//
	//		// verify
	//		assertEquals(0, mockDispatcherAudienceResponseContent.dispatchCallCount);
	//		assertEquals("desturl", mockNetworkService.connectUrlAsyncParametersUrl);
	//		assertEquals(HttpCommand.GET, mockNetworkService.connectUrlAsyncParametersCommand);
	//	}
	//
	//	// =================================================================================================================
	//	// helper methods
	//	// =================================================================================================================
	//	private LocalStorageService.DataStore getAudienceDataStore() {
	//		return fakeLocalStorageService.getDataStore(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_DATA_STORE);
	//	}

	private Map<String, Object> getFakeAamTraitsEventData() {
		return getFakeAamTraitsEventData(null);
	}

	private Map<String, Object> getFakeAamTraitsEventData(final Map<String, String> additionalTraits) {
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(AudienceTestConstants.EventDataKeys.Audience.VISITOR_TRAITS, traits(additionalTraits));

		return eventData;
	}

	private Map<String, Object> getFakeConfigEventData() {
		final Map<String, Object> fakeConfigData = new HashMap<>();
		fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
		fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "server");
		fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT, 4);
		fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING, false);

		return fakeConfigData;
	}

	private Map<String, Object> getFakeIdentityEventData() {
		final Map<String, Object> fakeIdentityData = new HashMap<>();
		fakeIdentityData.put(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testMarketingID");
		fakeIdentityData.put(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_BLOB, "testBlob");
		fakeIdentityData.put(AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_LOCATION_HINT, "testLocationHint");

		List<Map<String, Object>> visitorIDList = new ArrayList<>();
		visitorIDList.add(
			new HashMap<String, Object>() {
				{
					put("ID", "id1");
					put("ID_ORIGIN", "d_cid_ic");
					put("ID_TYPE", "id_type1");
					put("STATE", 1); // authenticated
				}
			}
		);
		visitorIDList.add(
			new HashMap<String, Object>() {
				{
					put("ID", "id2");
					put("ID_ORIGIN", "d_cid_ic");
					put("ID_TYPE", "id_type2");
					put("STATE", 2); // logged out
				}
			}
		);
		visitorIDList.add(
			new HashMap<String, Object>() {
				{
					put("ID", null);
					put("ID_ORIGIN", "d_cid_ic");
					put("ID_TYPE", "id_type3");
					put("STATE", 2); // logged out
				}
			}
		);
		visitorIDList.add(
			new HashMap<String, Object>() {
				{
					put("ID", "id4");
					put("ID_ORIGIN", "d_cid_ic");
					put("ID_TYPE", "id_type4");
					put("STATE", 0); // unknown
				}
			}
		);

		fakeIdentityData.put(AudienceTestConstants.EventDataKeys.Identity.VISITOR_IDS_LIST, visitorIDList);

		return fakeIdentityData;
	}

	private Map<String, Object> getFakeLifecycleEventData() {
		final Map<String, Object> fakeLifecycleData = new HashMap<>();
		fakeLifecycleData.put(
			AudienceTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA,
			fakeLifeCycleData()
		);
		return fakeLifecycleData;
	}

	private Event getSubmitSignalEvent(final Map<String, Object> eventData) {
		return new Event.Builder("TEST", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			.setEventData(eventData)
			.build();
	}

	private Event getLifecycleEvent(final Map<String, Object> eventData) {
		return new Event.Builder("TEST", EventType.LIFECYCLE, EventSource.REQUEST_CONTENT)
			.setEventData(eventData)
			.build();
	}

	private void setAudienceManagerStateProperties() {
		when(mockState.getUuid()).thenReturn("testuuid");
		HashMap<String, String> visitorProfile = new HashMap<>();
		visitorProfile.put("someKey", "someValue");
		when(mockState.getVisitorProfile()).thenReturn(visitorProfile);
		when(mockState.getStateData()).thenCallRealMethod(); // allows for calls to mocked getters
	}

	private HashMap<String, String> traits(final Map<String, String> additionalTraits) {
		HashMap<String, String> traits = new HashMap<>();
		traits.put("traitKey", "traitValue");

		if (additionalTraits != null) {
			traits.putAll(additionalTraits);
		}

		return traits;
	}

	private JSONArray prepareStuffArray() throws JSONException {
		JSONArray stuffArray = new JSONArray();
		JSONObject stuffValid1 = new JSONObject();
		stuffValid1.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "aud");
		stuffValid1.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "seg=mobile_android");
		stuffValid1.put("ttl", 0);
		stuffValid1.put("dmn", "audidute.com");
		JSONObject stuffValid2 = new JSONObject();
		stuffValid2.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "cookieKey");
		stuffValid2.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "cookieValue");
		stuffArray.put(stuffValid1);
		stuffArray.put(stuffValid2);
		return stuffArray;
	}

	private JSONArray prepareDestArray() throws JSONException {
		JSONArray destArray = new JSONArray();
		JSONObject destURL = new JSONObject();
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

	private void assertOptOutEvent(final Event responseEvent, final boolean hitSent) {
		assertNotNull(responseEvent);
		assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
		assertEquals(EventSource.RESPONSE_CONTENT, responseEvent.getSource());
		assertNull(responseEvent.getResponseID()); // not a paired response
		assertEquals(1, responseEvent.getEventData().size());
		assertEquals(
			new HashMap<String, Object>() {
				{
					put(AudienceConstants.EventDataKeys.Audience.OPTED_OUT_HIT_SENT, hitSent);
				}
			},
			responseEvent.getEventData()
		);
	}
}
