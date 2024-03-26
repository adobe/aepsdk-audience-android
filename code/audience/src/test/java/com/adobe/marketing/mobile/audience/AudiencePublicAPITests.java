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

package com.adobe.marketing.mobile.audience;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Audience;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.DataReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AudiencePublicAPITests {

    private static final String GRADLE_PROPERTIES_PATH = "../gradle.properties";
    private static final String PROPERTY_MODULE_VERSION = "moduleVersion";

    private MockedStatic<MobileCore> mockCore;

    @Before
    public void setup() {
        mockCore = mockStatic(MobileCore.class);
    }

    @After
    public void tearDown() {
        mockCore.close();
    }

    @Test
    public void testExtensionVersion_verifyModuleVersionInPropertiesFile_asEqual() {
        Properties properties = loadProperties(GRADLE_PROPERTIES_PATH);

        assertNotNull(Audience.extensionVersion());
        assertFalse(Audience.extensionVersion().isEmpty());

        String moduleVersion = properties.getProperty(PROPERTY_MODULE_VERSION);
        assertNotNull(moduleVersion);

        assertEquals(moduleVersion, Audience.extensionVersion());
    }

    @Test
    public void testGetVisitorProfile_whenNullCallback_doesNotDispatchRequest() {
        Audience.getVisitorProfile(null);

        mockCore.verify(
                () ->
                        MobileCore.dispatchEventWithResponseCallback(
                                any(Event.class), anyLong(), any(AdobeCallbackWithError.class)),
                never());
        mockCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void testGetVisitorProfile_dispatchesAudienceIdentityRequest() {
        Audience.getVisitorProfile(returnedVisitorProfile -> {});

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        mockCore.verify(
                () ->
                        MobileCore.dispatchEventWithResponseCallback(
                                eventCaptor.capture(),
                                eq(5000L),
                                any(AdobeCallbackWithError.class)),
                times(1));
        assertEquals("AudienceRequestIdentity", eventCaptor.getValue().getName());
        assertEquals(EventType.AUDIENCEMANAGER, eventCaptor.getValue().getType());
        assertEquals(EventSource.REQUEST_IDENTITY, eventCaptor.getValue().getSource());
    }

    @Test
    public void testSignalWithData_whenValidData_dispatchesAudienceContentRequest() {
        Map<String, String> sData = new HashMap<>();
        sData.put("key1", "value1");
        sData.put("key2", "value2");
        Audience.signalWithData(sData, null);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        mockCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()), times(1));
        assertEquals("AudienceRequestContent", eventCaptor.getValue().getName());
        assertEquals(EventType.AUDIENCEMANAGER, eventCaptor.getValue().getType());
        assertEquals(EventSource.REQUEST_CONTENT, eventCaptor.getValue().getSource());

        assertEquals(1, eventCaptor.getValue().getEventData().size());
        Map<String, String> traits =
                DataReader.optStringMap(eventCaptor.getValue().getEventData(), "aamtraits", null);
        assertNotNull(traits);
        assertEquals(sData, traits);
    }

    @Test
    public void testSignalWithData_whenEmptyData_dispatchesAudienceContentRequest() {
        Audience.signalWithData(new HashMap<>(), null);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        mockCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()), times(1));
        assertEquals("AudienceRequestContent", eventCaptor.getValue().getName());
        assertEquals(EventType.AUDIENCEMANAGER, eventCaptor.getValue().getType());
        assertEquals(EventSource.REQUEST_CONTENT, eventCaptor.getValue().getSource());

        assertEquals(1, eventCaptor.getValue().getEventData().size());
        Map<String, String> traits =
                DataReader.optStringMap(eventCaptor.getValue().getEventData(), "aamtraits", null);
        assertNotNull(traits);
        assertEquals(0, traits.size());
    }

    @Test
    public void
            testSignalWithData_whenCalledWithCallback_dispatchesAudienceContentRequestWithResponseCallback() {
        Map<String, String> sData = new HashMap<>();
        sData.put("key1", "value1");
        sData.put("key2", "value2");
        Audience.signalWithData(
                sData,
                new AdobeCallbackWithError<Map<String, String>>() {
                    @Override
                    public void fail(AdobeError adobeError) {}

                    @Override
                    public void call(Map<String, String> stringStringMap) {}
                });

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        mockCore.verify(
                () ->
                        MobileCore.dispatchEventWithResponseCallback(
                                eventCaptor.capture(),
                                eq(5000L),
                                any(AdobeCallbackWithError.class)),
                times(1));
        mockCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
        assertEquals("AudienceRequestContent", eventCaptor.getValue().getName());
        assertEquals(EventType.AUDIENCEMANAGER, eventCaptor.getValue().getType());
        assertEquals(EventSource.REQUEST_CONTENT, eventCaptor.getValue().getSource());

        assertEquals(1, eventCaptor.getValue().getEventData().size());
        Map<String, String> traits =
                DataReader.optStringMap(eventCaptor.getValue().getEventData(), "aamtraits", null);
        assertNotNull(traits);
        assertEquals(sData, traits);
    }

    @Test
    public void testReset_dispatchesAudienceResetRequest() {
        Audience.reset();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        mockCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()), times(1));

        assertEquals("AudienceRequestReset", eventCaptor.getValue().getName());
        assertEquals(EventType.AUDIENCEMANAGER, eventCaptor.getValue().getType());
        assertEquals(EventSource.REQUEST_RESET, eventCaptor.getValue().getSource());
        assertNull(eventCaptor.getValue().getEventData());
    }

    private Properties loadProperties(final String filepath) {
        Properties properties = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(filepath);

            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return properties;
    }
}
