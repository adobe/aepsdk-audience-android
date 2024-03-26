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

import static com.adobe.marketing.mobile.audience.AudienceTestConstants.EventDataKeys.Analytics.ANALYTICS_SERVER_RESPONSE_KEY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.PersistentHitQueue;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.SQLiteUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AudienceExtensionTests {

    private AudienceExtension audience;

    @Mock private ExtensionApi mockExtensionApi;

    @Mock private AudienceState mockState;

    @Mock private PersistentHitQueue mockDataQueue;

    @Mock private Networking mockNetworkService;

    @Mock private DeviceInforming mockDeviceInfoService;

    @Mock private ServiceProvider mockServiceProvider;

    private final MockedStatic<ServiceProvider> mockedStaticServiceProvider =
            Mockito.mockStatic(ServiceProvider.class);

    @Before
    public void setup() {
        mockedStaticServiceProvider
                .when(ServiceProvider::getInstance)
                .thenReturn(mockServiceProvider);
        when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
        when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
        audience = new AudienceExtension(mockExtensionApi, mockState, mockDataQueue);
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
        reset(mockState);
        reset(mockDataQueue);
        reset(mockNetworkService);
        reset(mockDeviceInfoService);
        mockedStaticServiceProvider.close();
        reset(mockServiceProvider);
    }

    @Test
    public void testOnRegistered_sharesAudienceSharedState() {
        // setup
        final HashMap<String, String> visitorProfile = new HashMap<>();
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
                DataReader.optString(
                        aamSharedState, AudienceTestConstants.EventDataKeys.Audience.UUID, ""));
        assertEquals(
                visitorProfile,
                DataReader.optStringMap(
                        aamSharedState,
                        AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                        null));
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
        final ArgumentCaptor<ExtensionEventListener> listenerCaptor =
                ArgumentCaptor.forClass(ExtensionEventListener.class);
        verify(mockExtensionApi, times(7))
                .registerEventListener(
                        eventTypeCaptor.capture(),
                        eventSourceCaptor.capture(),
                        listenerCaptor.capture());
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
    public void testOnRegistered_deletesDeprecatedV1HitDatabase() {
        final MockedStatic<SQLiteUtils> mockedSqliteUtils = Mockito.mockStatic(SQLiteUtils.class);

        audience.onRegistered();
        mockedSqliteUtils.verify(
                () -> SQLiteUtils.deleteDBFromCacheDir(eq("ADBMobileAAM.sqlite")), times(1));

        mockedSqliteUtils.close();
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
    public void
            testReadyForEvent_whenAudienceRequestOrLifecycle_whenConfigIdentitySet_returnsTrue() {
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        mockIdentitySharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeIdentityEventData()));
        assertTrue(audience.readyForEvent(getSubmitSignalEvent(getFakeAamTraitsEventData())));
        assertTrue(audience.readyForEvent(getLifecycleEvent(getFakeLifecycleEventData())));
    }

    @Test
    public void
            testReadyForEvent_whenAudienceRequestOrLifecycle_whenConfigSetIdentityPending_returnsFalse() {
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        mockIdentitySharedState(new SharedStateResult(SharedStateStatus.PENDING, null));
        assertFalse(audience.readyForEvent(getSubmitSignalEvent(getFakeAamTraitsEventData())));
        assertFalse(audience.readyForEvent(getLifecycleEvent(getFakeLifecycleEventData())));
    }

    @Test
    public void
            testReadyForEvent_whenAudienceRequestOrLifecycle_whenConfigPendingIdentitySet_returnsFalse() {
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.PENDING, null));
        mockIdentitySharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeIdentityEventData()));
        assertFalse(audience.readyForEvent(getSubmitSignalEvent(getFakeAamTraitsEventData())));
        assertFalse(audience.readyForEvent(getLifecycleEvent(getFakeLifecycleEventData())));
    }

    @Test
    public void
            testReadyForEvent_whenAudienceRequestOrLifecycle_whenConfigPendingIdentityPending_returnsFalse() {
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.PENDING, null));
        mockIdentitySharedState(new SharedStateResult(SharedStateStatus.PENDING, null));
        assertFalse(audience.readyForEvent(getSubmitSignalEvent(getFakeAamTraitsEventData())));
        assertFalse(audience.readyForEvent(getLifecycleEvent(getFakeLifecycleEventData())));
    }

    @Test
    public void testReadyForEvent_whenAnyOtherEvent_whenConfigSet_returnsTrue() {
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        assertTrue(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test1",
                                        EventType.AUDIENCEMANAGER,
                                        EventSource.REQUEST_IDENTITY)
                                .build()));
        assertTrue(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test2",
                                        EventType.AUDIENCEMANAGER,
                                        EventSource.REQUEST_RESET)
                                .build()));
        assertTrue(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test3",
                                        EventType.GENERIC_IDENTITY,
                                        EventSource.REQUEST_RESET)
                                .build()));
        assertTrue(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test4", EventType.ANALYTICS, EventSource.RESPONSE_CONTENT)
                                .build()));

        assertTrue(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test5",
                                        EventType.CONFIGURATION,
                                        EventSource.RESPONSE_CONTENT)
                                .build()));

        verify(mockExtensionApi, never())
                .getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any(SharedStateResolution.class));
    }

    @Test
    public void testReadyForEvent_whenAnyOtherEvent_whenConfigPending_returnsFalse() {
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.PENDING, null));

        assertFalse(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test1",
                                        EventType.AUDIENCEMANAGER,
                                        EventSource.REQUEST_IDENTITY)
                                .build()));
        assertFalse(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test2",
                                        EventType.AUDIENCEMANAGER,
                                        EventSource.REQUEST_RESET)
                                .build()));
        assertFalse(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test3",
                                        EventType.GENERIC_IDENTITY,
                                        EventSource.REQUEST_RESET)
                                .build()));
        assertFalse(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test4", EventType.ANALYTICS, EventSource.RESPONSE_CONTENT)
                                .build()));

        assertFalse(
                audience.readyForEvent(
                        new Event.Builder(
                                        "test5",
                                        EventType.CONFIGURATION,
                                        EventSource.RESPONSE_CONTENT)
                                .build()));

        verify(mockExtensionApi, never())
                .getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any(SharedStateResolution.class));
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
        assertEquals(
                testEvent.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        assertEquals(
                visitorProfile,
                DataReader.optStringMap(
                        responseEvent.getEventData(),
                        AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                        null));
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
        assertEquals(
                testEvent.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        assertTrue(
                responseEvent
                        .getEventData()
                        .containsKey(AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE));
        assertNull(
                DataReader.getStringMap(
                        responseEvent.getEventData(),
                        AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE));
    }

    @Test
    public void
            testHandleResetIdentities_whenAudienceReset_resetsInternalStateAndClearsSharedState() {
        // setup
        final Event testEvent =
                new Event.Builder(
                                "TestAAMReset",
                                EventType.AUDIENCEMANAGER,
                                EventSource.REQUEST_RESET)
                        .build();

        // test
        audience.handleResetIdentities(testEvent);

        verify(mockState).clearIdentifiers();
        verify(mockState).setLastResetTimestamp(eq(testEvent.getTimestamp()));
        verifyNoInteractions(mockDataQueue);

        // verify
        ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi)
                .createSharedState(sharedStateCaptor.capture(), eventCaptor.capture());
        Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
        assertNotNull(aamSharedState);
        assertEquals(0, aamSharedState.size());
        assertEquals(testEvent.getUniqueIdentifier(), eventCaptor.getValue().getUniqueIdentifier());
    }

    @Test
    public void
            testHandleResetIdentities_whenGenericIdentityReset_resetsInternalStateAndClearsSharedStateAndQueue() {
        // setup
        final Event testEvent =
                new Event.Builder(
                                "TestGenericReset",
                                EventType.GENERIC_IDENTITY,
                                EventSource.REQUEST_RESET)
                        .build();

        // test
        audience.handleResetIdentities(testEvent);

        verify(mockState).clearIdentifiers();
        verify(mockState).setLastResetTimestamp(eq(testEvent.getTimestamp()));
        verify(mockDataQueue).clear();

        // verify
        ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi)
                .createSharedState(sharedStateCaptor.capture(), eventCaptor.capture());
        Map<String, Object> aamSharedState = sharedStateCaptor.getValue();
        assertNotNull(aamSharedState);
        assertEquals(0, aamSharedState.size());
        assertEquals(testEvent.getUniqueIdentifier(), eventCaptor.getValue().getUniqueIdentifier());
    }

    @Test
    public void testHandleAnalyticsResponse_whenAAMForwardingEnabled_updatesLocalAndSharedState() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                ANALYTICS_SERVER_RESPONSE_KEY,
                "{"
                        + "'uuid':'12345', "
                        + "'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], "
                        + "'dests':[{'c':'https://www.adobe.com'}]}");
        Event testEvent = getAnalyticsResponseEvent(eventData);

        Map<String, Object> fakeConfig = getFakeConfigEventData();
        fakeConfig.put(
                AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
                true);
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfig));

        // test
        audience.handleAnalyticsResponse(testEvent);

        Map<String, String> visitorProfile = new HashMap<>();
        visitorProfile.put("cookieName", "key1=value1");

        // verifytedata
        verify(mockState).setUuid(eq("12345"));
        verify(mockState).setVisitorProfile(eq(visitorProfile));
        verifyNoInteractions(mockDataQueue);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi).createSharedState(any(), eventCaptor.capture());
        assertEquals(EventType.ANALYTICS, eventCaptor.getValue().getType());
        assertEquals(EventSource.RESPONSE_CONTENT, eventCaptor.getValue().getSource());
    }

    @Test
    public void testHandleAnalyticsResponse_whenAAMForwardingDisabled_ignoresEvent() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                ANALYTICS_SERVER_RESPONSE_KEY,
                "{"
                        + "'uuid':'12345', "
                        + "'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], "
                        + "'dests':[{'c':'https://www.adobe.com'}]}");
        Event testEvent = getAnalyticsResponseEvent(eventData);

        Map<String, Object> fakeConfig = getFakeConfigEventData();
        fakeConfig.put(
                AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
                false);
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfig));

        // test
        audience.handleAnalyticsResponse(testEvent);

        // verify
        verify(mockState, never()).setUuid(any(String.class));
        verify(mockState, never()).setVisitorProfile(any());
        verifyNoInteractions(mockDataQueue);
        verify(mockExtensionApi, never()).createSharedState(any(), any(Event.class));
    }

    @Test
    public void testHandleConfigurationResponse_whenPrivacyOptedIn_updatesInternalState() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optedin");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                        .setEventData(configuration)
                        .build();

        // test
        audience.handleConfigurationResponse(testEvent);

        //
        verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_IN));
        verify(mockDataQueue).handlePrivacyChange(eq(MobilePrivacyStatus.OPT_IN));
        verify(mockExtensionApi, never()).dispatch(any(Event.class));
        verify(mockExtensionApi).createSharedState(any(), any(Event.class));
    }

    @Test
    public void testHandleConfigurationResponse_whenPrivacyUnknown_updatesInternalState() {
        when(mockState.getUuid()).thenReturn("testuuid");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optunknown");
        configuration.put("audience.server", "server.com");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                        .setEventData(configuration)
                        .build();

        // test
        audience.handleConfigurationResponse(testEvent);

        //
        verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.UNKNOWN));
        verify(mockDataQueue).handlePrivacyChange(eq(MobilePrivacyStatus.UNKNOWN));
        verify(mockExtensionApi, never()).dispatch(any(Event.class));
        verify(mockExtensionApi).createSharedState(any(), any(Event.class));
        verifyNoInteractions(mockNetworkService);
    }

    @Test
    public void
            testHandleConfigurationResponse_whenPrivacyOptedOutNoUUIDAndNoAAMServer_updatesInternalStateAndDispatchesOptoutEvent() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optedout");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                        .setEventData(configuration)
                        .build();

        // test
        audience.handleConfigurationResponse(testEvent);

        // verify
        verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_OUT));
        verify(mockDataQueue).handlePrivacyChange(eq(MobilePrivacyStatus.OPT_OUT));
        verifyNoInteractions(mockNetworkService);
        verify(mockExtensionApi).createSharedState(any(), any(Event.class));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
        assertOptOutEvent(eventCaptor.getValue(), false);
    }

    @Test
    public void
            testHandleConfigurationResponse_whenPrivacyOptedOutAndAAMServer_sendsOptoutNetworkRequestAndDispatchesEvent() {
        // setup
        when(mockState.getUuid()).thenReturn("testuuid");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optedout");
        configuration.put("audience.server", "server.com");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                        .setEventData(configuration)
                        .build();

        // test
        audience.handleConfigurationResponse(testEvent);

        // verify
        verify(mockState).setMobilePrivacyStatus(eq(MobilePrivacyStatus.OPT_OUT));
        verify(mockDataQueue).handlePrivacyChange(eq(MobilePrivacyStatus.OPT_OUT));
        ArgumentCaptor<NetworkRequest> networkRequestCaptor =
                ArgumentCaptor.forClass(NetworkRequest.class);
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), notNull());
        assertEquals(
                "https://server.com/demoptout.jpg?d_uuid=testuuid",
                networkRequestCaptor.getValue().getUrl());
        assertEquals(HttpMethod.GET, networkRequestCaptor.getValue().getMethod());

        verify(mockExtensionApi).createSharedState(any(), any(Event.class));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
        assertOptOutEvent(eventCaptor.getValue(), true);
    }

    @Test
    public void
            testHandleConfigurationResponse_whenPrivacyOptedOutValidUUIDAndNoAAMServer_noNetworkRequestAndDispatchesOptoutEvent() {
        // setup
        when(mockState.getUuid()).thenReturn("testuuid");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optedout");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
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
    public void
            testHandleConfigurationResponse_whenPrivacyOptedOutNoUUIDAndValidAAMServer_noNetworkRequestAndDispatchesOptoutEvent() {
        // setup
        when(mockState.getUuid()).thenReturn(null);
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("global.privacy", "optedout");
        configuration.put("audience.server", "server.com");
        Event testEvent =
                new Event.Builder(
                                "TestConfig", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
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
    public void testHandleLifecycleResponse_whenAamForwardingEnabled_shouldIgnoreLifecycleEvent() {
        // setup
        final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData());
        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
                true);
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleLifecycleResponse(lifecycleEvent);

        // verify
        verifyNoInteractions(mockDataQueue);
    }

    @Test
    public void
            testHandleLifecycleResponse_whenAamForwardingDisabled_shouldProcessLifecycleEvent() {
        // setup
        final Event lifecycleEvent = getLifecycleEvent(getFakeLifecycleEventData());
        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
                false);
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleLifecycleResponse(lifecycleEvent);

        // verify
        verify(mockExtensionApi).createPendingSharedState(any(Event.class));
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("c_a_Launches=2&c_a_AppID=someAppID"));
    }

    @Test
    public void
            testHandleAudienceRequestContent_whenAudienceNotConfigured_dispatchesResponseWithNullProfile()
                    throws DataReaderException {
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, Collections.emptyMap()));

        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verifyNoInteractions(mockDataQueue);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi).dispatch(eventCaptor.capture());
        assertNull(
                DataReader.getStringMap(
                        eventCaptor.getValue().getEventData(),
                        AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE));
    }

    @Test
    public void testHandleAudienceRequestContent_whenMCOrgIDSet_queuesHit() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
                "testExperience@adobeorg");
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verify(mockExtensionApi, never()).dispatch(any(Event.class));
        verify(mockExtensionApi).createPendingSharedState(any(Event.class));
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("d_orgid=testExperience@adobeorg"));
    }

    @Test
    public void testHandleAudienceRequestContent_whenRequestIsValid_emptyQueue() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
                "testExperience@adobeorg");
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "server:_80");
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verify(mockExtensionApi, never()).dispatch(any(Event.class));
        verify(mockExtensionApi).createPendingSharedState(any(Event.class));
        verifyNoInteractions(mockDataQueue);
    }

    @Test
    public void testHandleAudienceRequestContent_whenNullDataQueue_doesNotCrash() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORGID,
                "testExperience@adobeorg");
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        when(mockServiceProvider.getDataQueueService()).thenReturn(null);
        audience = new AudienceExtension(mockExtensionApi, mockState, null);
        audience.handleAudienceRequestContent(event);

        // verify
        verify(mockExtensionApi, never()).dispatch(any(Event.class));
    }

    @Test
    public void testHandleAudienceRequestContent_whenPrivacyOptOut_dispatchesResponse()
            throws Exception {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY,
                "optedout");
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verifyNoInteractions(mockDataQueue);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi).dispatch(eventCaptor.capture());
        assertNull(
                DataReader.getStringMap(
                        eventCaptor.getValue().getEventData(),
                        AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE));
    }

    @Test
    public void testHandleAudienceRequestContent_whenPrivacyUnknown_queuesHit() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY,
                "optunknown");
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verify(mockDataQueue).queue(any(DataEntity.class));
    }

    @Test
    public void testHandleAudienceRequestContent_whenServerEmpty_dispatchesResponse() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final Map<String, Object> fakeConfigData = getFakeConfigEventData();
        fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "");
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, fakeConfigData));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        verify(mockExtensionApi).dispatch(any(Event.class));
    }

    @Test
    public void testHandleAudienceRequestContent_whenNullPlatformInfo_usesDefaultJava() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        when(mockDeviceInfoService.getCanonicalPlatformName()).thenReturn(null);
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(
                        new SharedStateResult(SharedStateStatus.SET, getFakeIdentityEventData()));

        setAudienceManagerStateProperties();

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("d_ptfm=java"));
    }

    @Test
    public void testHandleAudienceRequestContent_packagesAllParams() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        when(mockDeviceInfoService.getCanonicalPlatformName()).thenReturn("mockPlatform");
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(
                        new SharedStateResult(SharedStateStatus.SET, getFakeIdentityEventData()));

        setAudienceManagerStateProperties();

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("c_traitKey=traitValue"));
        assertTrue(audienceEntity.getUrl().contains("d_uuid=testuuid"));
        assertTrue(audienceEntity.getUrl().contains("d_ptfm=mockPlatform"));
        assertTrue(audienceEntity.getUrl().contains("d_dst=1"));
        assertTrue(audienceEntity.getUrl().contains("d_rtbd=json"));
        assertTrue(audienceEntity.getUrl().contains("d_mid=testMarketingID"));
        assertTrue(audienceEntity.getUrl().contains("d_blob=testBlob"));
        assertTrue(audienceEntity.getUrl().contains("dcs_region=testLocationHint"));
        final String expectedCustomIds =
                "d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010";
        assertTrue(
                String.format(
                        "Identity params assert failed. \n"
                                + "URL received: %s, \n"
                                + "Expected contains: %s",
                        audienceEntity.getUrl(), expectedCustomIds),
                audienceEntity.getUrl().contains(expectedCustomIds));
        assertEquals(4, audienceEntity.getTimeoutSec());
    }

    @Test
    public void testHandleAudienceRequestContent_whenNoTraitsNoCustomIds_packagesAllOtherParams() {
        // setup
        final Event event = getSubmitSignalEvent(null);

        when(mockDeviceInfoService.getCanonicalPlatformName()).thenReturn("mockPlatform");
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        Map<String, Object> identityData = getFakeIdentityEventData();
        identityData.remove(AudienceTestConstants.EventDataKeys.Identity.VISITOR_IDS_LIST);
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, identityData));

        setAudienceManagerStateProperties();

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertFalse(audienceEntity.getUrl().contains("c_"));
        assertTrue(audienceEntity.getUrl().contains("d_uuid=testuuid"));
        assertTrue(audienceEntity.getUrl().contains("d_ptfm=mockPlatform"));
        assertTrue(audienceEntity.getUrl().contains("d_dst=1"));
        assertTrue(audienceEntity.getUrl().contains("d_rtbd=json"));
        assertTrue(audienceEntity.getUrl().contains("d_mid=testMarketingID"));
        assertTrue(audienceEntity.getUrl().contains("d_blob=testBlob"));
        assertTrue(audienceEntity.getUrl().contains("dcs_region=testLocationHint"));
        assertFalse(
                audienceEntity
                        .getUrl()
                        .contains(
                                "d_cid_ic=id_type1%01id1%011&d_cid_ic=id_type2%01id2%012&d_cid_ic=id_type3%012&d_cid_ic=id_type4%01id4%010"));
        assertEquals(4, audienceEntity.getTimeoutSec());
    }

    @Test
    public void testHandleAudienceRequestContent_whenTraitWithNullValue_skipsTrait() {
        // setup
        final HashMap<String, String> additionalTraits = new HashMap<>();
        additionalTraits.put("nullKey", null);
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits));
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(
                        new SharedStateResult(SharedStateStatus.SET, getFakeIdentityEventData()));

        setAudienceManagerStateProperties();

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("c_traitKey=traitValue"));
        assertFalse(audienceEntity.getUrl().contains("c_nullKey="));
        assertEquals(4, audienceEntity.getTimeoutSec());
    }

    @Test
    public void
            testHandleAudienceRequestContent_whenNullIdentitySharedState_doesNotIncludeIdentityFields() {
        // setup
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, null));

        setAudienceManagerStateProperties();

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().startsWith("https://server/event?"));
        assertTrue(audienceEntity.getUrl().contains("c_traitKey=traitValue"));
        assertTrue(audienceEntity.getUrl().contains("d_uuid=testuuid"));
        assertTrue(audienceEntity.getUrl().contains("d_dst=1"));
        assertTrue(audienceEntity.getUrl().contains("d_rtbd=json"));
        assertFalse(audienceEntity.getUrl().contains("d_mid"));
        assertFalse(audienceEntity.getUrl().contains("d_blob"));
        assertFalse(audienceEntity.getUrl().contains("dcs_region"));
        assertFalse(audienceEntity.getUrl().contains("d_cid_ic"));
        assertEquals(4, audienceEntity.getTimeoutSec());
    }

    @Test
    public void testHandleAudienceRequestContent_sanitizesTraitKeys() {
        // setup
        final HashMap<String, String> additionalTraits = new HashMap<>();
        additionalTraits.put("trait.key", "trait.value");
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits));

        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertTrue(audienceEntity.getUrl().contains("c_trait_key=trait.value"));
    }

    @Test
    public void testHandleAudienceRequestContent_whenEmptyTraitKeys_skipsTheseKeys() {
        // setup
        final HashMap<String, String> additionalTraits = new HashMap<>();
        additionalTraits.put("", "traitvalue");
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData(additionalTraits));
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.handleAudienceRequestContent(event);

        // verify
        ArgumentCaptor<DataEntity> entityCaptor = ArgumentCaptor.forClass(DataEntity.class);
        verify(mockDataQueue).queue(entityCaptor.capture());
        AudienceDataEntity audienceEntity =
                AudienceDataEntity.fromDataEntity(entityCaptor.getValue());
        assertNotNull(audienceEntity);
        assertFalse(audienceEntity.getUrl().contains("c_=traitvalue"));
    }

    // =================================================================================================================
    // AudienceNetworkResponseHandler tests
    // =================================================================================================================
    @Test
    public void testNetworkResponseHandler_whenResponseEmpty_dispatchesResponseEvent() {
        // setup
        final String mockResponse = "";
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());

        // test
        audience.networkResponseHandler.complete(mockResponse, event);

        // verify
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
        final Event responseEvent = eventCaptor.getValue();
        assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, responseEvent.getSource());
        assertEquals(
                event.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        assertTrue(
                DataReader.optStringMap(
                                responseEvent.getEventData(),
                                AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                                null)
                        .isEmpty());
    }

    @Test
    public void
            testNetworkResponseHandler_whenResponseWithUUIDNoProfile_savesUUIDAndDispatchesResponseEvent() {
        // setup
        final String mockResponse = "{'uuid':'testuuid'}";
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(mockResponse, event);

        // verify
        verify(mockState).setUuid(eq("testuuid"));
        verify(mockExtensionApi, times(1)).dispatch(any(Event.class));
    }

    @Test
    public void
            testNetworkResponseHandler_whenResponseWithEmptyStuffArray_dispatchesResponseEvent() {
        // setup
        final String mockResponse = "{'stuff':[]}";
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(mockResponse, event);

        // verify
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
        final Event responseEvent = eventCaptor.getValue();
        assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, responseEvent.getSource());
        assertEquals(
                event.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        assertTrue(
                DataReader.optStringMap(
                                responseEvent.getEventData(),
                                AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                                null)
                        .isEmpty());
    }

    @Test
    public void
            testNetworkResponseHandler_whenResponseValidStuffArray_dispatchesResponseEventsWithProfile()
                    throws Exception {
        // setup
        final String stuffArrayAsString = prepareStuffArray().toString();
        final String mockResponse = "{'stuff':" + stuffArrayAsString + "}";
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(mockResponse, event);

        // verify
        ArgumentCaptor<Map<String, String>> visitorProfileCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockState).setVisitorProfile(visitorProfileCaptor.capture());
        assertEquals(2, visitorProfileCaptor.getValue().size());
        assertEquals("cookieValue", visitorProfileCaptor.getValue().get("cookieKey"));
        assertEquals("seg=mobile_android", visitorProfileCaptor.getValue().get("aud"));

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // expect 2 events to be dispatched, one paired and one for generic listeners
        verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());
        final Event pairedResponseEvent = eventCaptor.getAllValues().get(0);
        assertNull(
                event.getUniqueIdentifier(),
                pairedResponseEvent.getResponseID()); // generic response, not paired
        final Event responseEvent = eventCaptor.getAllValues().get(1);
        assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, responseEvent.getSource());
        assertEquals(
                event.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        Map<String, String> dispatchedVisitorProfile =
                DataReader.optStringMap(
                        responseEvent.getEventData(),
                        AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                        null);
        assertEquals(2, dispatchedVisitorProfile.size());
        assertEquals("seg=mobile_android", dispatchedVisitorProfile.get("aud"));
        assertEquals("cookieValue", dispatchedVisitorProfile.get("cookieKey"));
    }

    @Test
    public void
            testNetworkResponseHandler_whenResponseValidDestArray_dispatchesResponseAndNetworkRequest()
                    throws Exception {
        // setup
        final String destsArrayAsString = prepareDestArray().toString();
        final String mockResponse = "{'dests':" + destsArrayAsString + "}";
        final Event event = getSubmitSignalEvent(getFakeAamTraitsEventData());
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(mockResponse, event);

        // verify
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
        final Event responseEvent = eventCaptor.getValue();
        assertEquals(EventType.AUDIENCEMANAGER, responseEvent.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, responseEvent.getSource());
        assertEquals(
                event.getUniqueIdentifier(),
                responseEvent.getResponseID()); // verifies in response to request event
        assertEquals(1, responseEvent.getEventData().size());
        assertTrue(
                DataReader.optStringMap(
                                responseEvent.getEventData(),
                                AudienceTestConstants.EventDataKeys.Audience.VISITOR_PROFILE,
                                null)
                        .isEmpty());
        ArgumentCaptor<NetworkRequest> networkRequestCaptor =
                ArgumentCaptor.forClass(NetworkRequest.class);
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), notNull());
        assertEquals("desturl", networkRequestCaptor.getValue().getUrl());
        assertEquals(HttpMethod.GET, networkRequestCaptor.getValue().getMethod());
    }

    // =================================================================================================================
    // protected HashMap<String, String> processResponse(final String response, final Event event)
    // =================================================================================================================
    @Test
    public void testNetworkResponseHandler_whenAllParams_responseIsProperlyProcessed() {
        // setup
        final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final String jsonResponse =
                "{"
                        + "'uuid':'12345', "
                        + "'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], "
                        + "'dests':[{'c':'https://www.adobe.com'}]}";
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(jsonResponse, testEvent);

        // verify
        verify(mockState).setUuid(eq("12345"));
        ArgumentCaptor<Map<String, String>> visitorProfileCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockState).setVisitorProfile(visitorProfileCaptor.capture());
        assertEquals(1, visitorProfileCaptor.getValue().size());
        assertEquals("key1=value1", visitorProfileCaptor.getValue().get("cookieName"));
        verify(mockNetworkService)
                .connectAsync(
                        any(NetworkRequest.class),
                        any(NetworkCallback.class)); // the dest was properly forwarded
    }

    @Test
    public void testNetworkResponseHandler_whenNullResponse_ignoresResponse() {
        // setup
        final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final String jsonResponse = null;
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(jsonResponse, testEvent);

        // verify
        verify(mockState, never()).setUuid(any(String.class));
        verify(mockState, never()).setVisitorProfile(any());
        verifyNoInteractions(mockNetworkService);
        verify(mockExtensionApi, never()).createSharedState(any(), any(Event.class));
    }

    @Test
    public void testNetworkResponseHandler_whenEmptyResponse_ignoresResponse() {
        // setup
        final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final String jsonResponse = "";
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(jsonResponse, testEvent);

        // verify
        verify(mockState, never()).setUuid(any(String.class));
        verify(mockState, never()).setVisitorProfile(any());
        verifyNoInteractions(mockNetworkService);
        verify(mockExtensionApi, never()).createSharedState(any(), any(Event.class));
    }

    @Test
    public void testNetworkResponseHandler_whenNoConfiguration_doesNothing() {
        // setup
        final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final String jsonResponse =
                "{"
                        + "'uuid':'12345', "
                        + "'stuff':[{'cn':'cookieName', 'cv':'key1=value1'}], "
                        + "'dests':[{'c':'https://www.adobe.com'}]}";
        mockConfigSharedState(new SharedStateResult(SharedStateStatus.PENDING, null));

        // test
        audience.networkResponseHandler.complete(jsonResponse, testEvent);

        // verify
        verify(mockState, never()).setUuid(any(String.class));
        verify(mockState, never()).setVisitorProfile(any());
        verifyNoInteractions(mockNetworkService);
        verify(mockExtensionApi, never()).createSharedState(any(), any(Event.class));
        verify(mockExtensionApi).dispatch(any(Event.class)); // dispatches response content
    }

    @Test
    public void testNetworkResponseHandler_whenMalformedJSON_doesNothing() {
        // setup
        final Event testEvent = getSubmitSignalEvent(getFakeAamTraitsEventData());
        final String jsonResponse =
                "{"
                        + "'uuid':'12345', "
                        + "'stuff':{'cn':'cookieName', 'cv':'key1=value1'}], "
                        + "'dests':[{'c':'https://www.adobe.com']";
        mockConfigSharedState(
                new SharedStateResult(SharedStateStatus.SET, getFakeConfigEventData()));

        // test
        audience.networkResponseHandler.complete(jsonResponse, testEvent);

        // verify
        verify(mockState, never()).setUuid(any(String.class));
        verify(mockState, never()).setVisitorProfile(any());
        verifyNoInteractions(mockNetworkService);
        verify(mockExtensionApi, never()).createSharedState(any(), any(Event.class));
        verify(mockExtensionApi)
                .dispatch(any(Event.class)); // dispatches response content with empty data
    }

    private Map<String, Object> getFakeAamTraitsEventData() {
        return getFakeAamTraitsEventData(null);
    }

    private Map<String, Object> getFakeAamTraitsEventData(
            final Map<String, String> additionalTraits) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                AudienceTestConstants.EventDataKeys.Audience.VISITOR_TRAITS,
                traits(additionalTraits));

        return eventData;
    }

    private Map<String, Object> getFakeConfigEventData() {
        final Map<String, Object> fakeConfigData = new HashMap<>();
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_SERVER, "server");
        fakeConfigData.put(AudienceTestConstants.EventDataKeys.Configuration.AAM_CONFIG_TIMEOUT, 4);
        fakeConfigData.put(
                AudienceTestConstants.EventDataKeys.Configuration.ANALYTICS_CONFIG_AAMFORWARDING,
                false);

        return fakeConfigData;
    }

    private Map<String, Object> getFakeIdentityEventData() {
        final Map<String, Object> fakeIdentityData = new HashMap<>();
        fakeIdentityData.put(
                AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testMarketingID");
        fakeIdentityData.put(
                AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_BLOB, "testBlob");
        fakeIdentityData.put(
                AudienceTestConstants.EventDataKeys.Identity.VISITOR_ID_LOCATION_HINT,
                "testLocationHint");

        List<Map<String, Object>> visitorIDList = new ArrayList<>();
        visitorIDList.add(
                new HashMap<String, Object>() {
                    {
                        put("ID", "id1");
                        put("ID_ORIGIN", "d_cid_ic");
                        put("ID_TYPE", "id_type1");
                        put("STATE", 1); // authenticated
                    }
                });
        visitorIDList.add(
                new HashMap<String, Object>() {
                    {
                        put("ID", "id2");
                        put("ID_ORIGIN", "d_cid_ic");
                        put("ID_TYPE", "id_type2");
                        put("STATE", 2); // logged out
                    }
                });
        visitorIDList.add(
                new HashMap<String, Object>() {
                    {
                        put("ID", null);
                        put("ID_ORIGIN", "d_cid_ic");
                        put("ID_TYPE", "id_type3");
                        put("STATE", 2); // logged out
                    }
                });
        visitorIDList.add(
                new HashMap<String, Object>() {
                    {
                        put("ID", "id4");
                        put("ID_ORIGIN", "d_cid_ic");
                        put("ID_TYPE", "id_type4");
                        put("STATE", 0); // unknown
                    }
                });

        fakeIdentityData.put(
                AudienceTestConstants.EventDataKeys.Identity.VISITOR_IDS_LIST, visitorIDList);

        return fakeIdentityData;
    }

    private Map<String, Object> getFakeLifecycleEventData() {
        final Map<String, Object> fakeLifecycleData = new HashMap<>();
        fakeLifecycleData.put(
                AudienceTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA,
                fakeLifeCycleData());
        return fakeLifecycleData;
    }

    private Event getSubmitSignalEvent(final Map<String, Object> eventData) {
        return new Event.Builder("TEST", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
    }

    private Event getLifecycleEvent(final Map<String, Object> eventData) {
        return new Event.Builder("TEST", EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .build();
    }

    private Event getAnalyticsResponseEvent(final Map<String, Object> eventData) {
        return new Event.Builder(
                        "TestAnalyticsResponse", EventType.ANALYTICS, EventSource.RESPONSE_CONTENT)
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
        stuffValid1.put(
                AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "seg=mobile_android");
        stuffValid1.put("ttl", 0);
        stuffValid1.put("dmn", "audidute.com");
        JSONObject stuffValid2 = new JSONObject();
        stuffValid2.put(AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_NAME_KEY, "cookieKey");
        stuffValid2.put(
                AudienceTestConstants.AUDIENCE_MANAGER_JSON_COOKIE_VALUE_KEY, "cookieValue");
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
        HashMap<String, String> lifecycleData = new HashMap<>();
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
                responseEvent.getEventData());
    }

    private void mockConfigSharedState(final SharedStateResult sharedStateResult) {
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Configuration.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(sharedStateResult);
    }

    private void mockIdentitySharedState(final SharedStateResult sharedStateResult) {
        when(mockExtensionApi.getSharedState(
                        eq(AudienceTestConstants.EventDataKeys.Identity.MODULE_NAME),
                        any(Event.class),
                        eq(false),
                        any()))
                .thenReturn(sharedStateResult);
    }
}
