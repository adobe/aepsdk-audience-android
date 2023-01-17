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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AudienceCoreTest {

	private AudienceCore core;
	private MockEventHubUnitTest eventHub;

	@Before
	public void setup() {
		PlatformServices fakePlatformServices = new FakePlatformServices();
		eventHub = new MockEventHubUnitTest("MockEventHubUnitTest", fakePlatformServices);
		core =
			new AudienceCore(
				eventHub,
				new ModuleDetails() {
					@Override
					public String getName() {
						return "AudienceExtension";
					}

					@Override
					public String getVersion() {
						return "AudienceExtensionVersion";
					}

					@Override
					public Map<String, String> getAdditionalInfo() {
						return null;
					}
				}
			);
	}

	@Test
	public void setDataProviderIds_should_dispatchAudienceIdentityRequest() throws Exception {
		String testDpid = "1212";
		String testDpuuid = "1234";
		core.setDataProviderIds(testDpid, testDpuuid);

		EventData data = eventHub.dispatchedEvent.getData();

		assertEquals(2, data.size());
		assertEquals("1212", data.getString2("dpid"));
		assertEquals("1234", data.getString2("dpuuid"));
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceSetDataProviderIds", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_IDENTITY,
			eventHub.dispatchedEvent.getEventSource()
		);
		assertNotNull("event has eventData", eventHub.dispatchedEvent.getData());
	}

	@Test
	public void getDpidAndDpuuid_should_dispatchAudienceIdentityRequest() {
		core.getDpidAndDpuuid(
			new AdobeCallback<Map<String, String>>() {
				@Override
				public void call(Map<String, String> value) {}
			}
		);

		EventData data = eventHub.dispatchedEvent.getData();

		assertEquals(0, data.size());
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceRequestIdentity", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_IDENTITY,
			eventHub.dispatchedEvent.getEventSource()
		);
		assertTrue("one-time listener is registered for callback", eventHub.registerOneTimeListenerWithErrorCalled);
		assertNotNull("one-time listener has a callback block", eventHub.registerOneTimeListenerWithErrorParamBlock);
	}

	@Test
	public void getVisitorProfile_should_dispatchAudienceIdentityRequest() {
		core.getVisitorProfile(
			new AdobeCallback<Map<String, String>>() {
				@Override
				public void call(Map<String, String> value) {}
			}
		);

		EventData data = eventHub.dispatchedEvent.getData();

		assertEquals(0, data.size());
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceRequestIdentity", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_IDENTITY,
			eventHub.dispatchedEvent.getEventSource()
		);
		assertTrue("one-time listener is registered for callback", eventHub.registerOneTimeListenerWithErrorCalled);
		assertNotNull("one-time listener has a callback block", eventHub.registerOneTimeListenerWithErrorParamBlock);
	}

	@Test
	public void signalWithData_should_dispatchAudienceContentRequest() throws VariantException {
		Map<String, String> sData = new HashMap<String, String>();
		sData.put("key1", "value1");
		sData.put("key2", "value2");
		core.submitAudienceProfileData(
			sData,
			new AdobeCallback<Map<String, String>>() {
				@Override
				public void call(Map<String, String> value) {}
			}
		);

		EventData data = eventHub.dispatchedEvent.getData();
		Map<String, String> traits = data.getStringMap("aamtraits");

		assertEquals(1, data.size());
		assertEquals(2, traits.size());
		assertEquals("value1", traits.get("key1"));
		assertEquals("value2", traits.get("key2"));
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceRequestContent", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_CONTENT,
			eventHub.dispatchedEvent.getEventSource()
		);
		assertTrue("one-time listener is registered for callback", eventHub.registerOneTimeListenerWithErrorCalled);
		assertNotNull("one-time listener has a callback block", eventHub.registerOneTimeListenerWithErrorParamBlock);
	}

	@Test
	public void signalWithEmptyData_should_dispatchAudienceContentRequest() throws VariantException {
		core.submitAudienceProfileData(
			new HashMap<String, String>(),
			new AdobeCallback<Map<String, String>>() {
				@Override
				public void call(Map<String, String> value) {}
			}
		);

		EventData data = eventHub.dispatchedEvent.getData();
		Map<String, String> traits = data.getStringMap("aamtraits");

		assertEquals(1, data.size());
		assertEquals(0, traits.size());
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceRequestContent", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_CONTENT,
			eventHub.dispatchedEvent.getEventSource()
		);
		assertTrue("one-time listener is registered for callback", eventHub.registerOneTimeListenerWithErrorCalled);
		assertNotNull("one-time listener has a callback block", eventHub.registerOneTimeListenerWithErrorParamBlock);
	}

	@Test
	public void reset_should_dispatchAudienceResetRequest() {
		core.reset();
		EventData data = eventHub.dispatchedEvent.getData();

		assertEquals(0, data.size());
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);
		assertEquals("event has correct name", "AudienceRequestReset", eventHub.dispatchedEvent.getName());
		assertEquals(
			"event has correct event type",
			EventType.AUDIENCEMANAGER,
			eventHub.dispatchedEvent.getEventType()
		);
		assertEquals(
			"event has correct event source",
			EventSource.REQUEST_RESET,
			eventHub.dispatchedEvent.getEventSource()
		);
	}
}
