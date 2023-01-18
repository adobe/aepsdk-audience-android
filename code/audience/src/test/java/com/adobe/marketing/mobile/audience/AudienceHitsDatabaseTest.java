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

import java.net.HttpURLConnection;
import org.junit.Before;
import org.junit.Test;

public class AudienceHitsDatabaseTest extends BaseTest {

	private AudienceHitsDatabase audienceHitsDatabase;
	private MockAudienceExtension parentModule;
	private MockNetworkService networkService;
	private MockHitQueue<AudienceHit, AudienceHitSchema> hitQueue;

	@Before
	public void setup() {
		super.beforeEach();
		hitQueue = new MockHitQueue<AudienceHit, AudienceHitSchema>(platformServices);
		parentModule = new MockAudienceExtension(eventHub, platformServices);
		networkService = platformServices.getMockNetworkService();
		audienceHitsDatabase = new AudienceHitsDatabase(parentModule, platformServices, hitQueue);
	}

	@Test
	public void testProcess_NotRetry_When_ConnectionIsNull() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		networkService.connectUrlReturnValue = null;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.NO, retryType);
		assertTrue(parentModule.handleNetworkResponseWasCalled);
		assertNull(parentModule.handleNetworkResponseParamResponse);
		assertEquals(EventType.AUDIENCEMANAGER, parentModule.handleNetworkResponseParamEvent.getEventType());
		assertEquals(EventSource.REQUEST_CONTENT, parentModule.handleNetworkResponseParamEvent.getEventSource());
		assertEquals("pairId", parentModule.handleNetworkResponseParamEvent.getResponsePairID());
		assertEquals(3, parentModule.handleNetworkResponseParamEvent.getEventNumber());
		assertEquals(3000, parentModule.handleNetworkResponseParamEvent.getTimestamp());
	}

	@Test
	public void testProcess_NotRetry_When_ResponseIsValid() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", 200, null, null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.NO, retryType);
		assertTrue(parentModule.handleNetworkResponseWasCalled);
		assertEquals("", parentModule.handleNetworkResponseParamResponse);
		assertEquals(EventType.AUDIENCEMANAGER, parentModule.handleNetworkResponseParamEvent.getEventType());
		assertEquals(EventSource.REQUEST_CONTENT, parentModule.handleNetworkResponseParamEvent.getEventSource());
		assertEquals("pairId", parentModule.handleNetworkResponseParamEvent.getResponsePairID());
		assertEquals(3, parentModule.handleNetworkResponseParamEvent.getEventNumber());
		assertEquals(3000, parentModule.handleNetworkResponseParamEvent.getTimestamp());
	}

	@Test
	public void testProcess_Retry_When_ConnectionTimeOout() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_CLIENT_TIMEOUT, null, null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}

	@Test
	public void testProcess_Retry_When_GateWayOout() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_GATEWAY_TIMEOUT, null, null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}

	@Test
	public void testProcess_Retry_When_HttpUnavailable() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_UNAVAILABLE, null, null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}

	@Test
	public void testProcess_NotRetry_When_OtherRepsonseCode() throws Exception {
		AudienceHit audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", 301, null, null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.NO, retryType);
		assertTrue(parentModule.handleNetworkResponseWasCalled);
		assertEquals(null, parentModule.handleNetworkResponseParamResponse);
		assertEquals(EventType.AUDIENCEMANAGER, parentModule.handleNetworkResponseParamEvent.getEventType());
	}

	@Test
	public void testQueue_When_PrivacyOptIN() throws Exception {
		Event event = new Event.Builder("AAM Request", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			.setResponsePairID("pairId")
			.setTimestamp(123456000)
			.build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.OPT_IN, event);
		AudienceHit audienceHit = hitQueue.queueParametersHit;
		assertEquals("url", audienceHit.url);
		assertEquals(5, audienceHit.timeout);
		assertEquals(123456, audienceHit.timestamp);
		assertEquals("pairId", audienceHit.pairId);
		assertEquals(20, audienceHit.eventNumber);

		assertTrue(hitQueue.bringOnlineWasCalled);
	}

	@Test
	public void testQueue_When_PrivacyOptOut() throws Exception {
		Event event = new Event.Builder("AAM Request", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			.setResponsePairID("pairId")
			.setTimestamp(123456000)
			.build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.OPT_OUT, event);
		AudienceHit audienceHit = hitQueue.queueParametersHit;
		assertEquals("url", audienceHit.url);
		assertEquals(5, audienceHit.timeout);
		assertEquals(123456, audienceHit.timestamp);
		assertEquals("pairId", audienceHit.pairId);
		assertEquals(20, audienceHit.eventNumber);

		assertFalse(hitQueue.bringOnlineWasCalled);
	}

	@Test
	public void testQueue_When_PrivacyOptUnknown() throws Exception {
		Event event = new Event.Builder("AAM Request", EventType.AUDIENCEMANAGER, EventSource.REQUEST_CONTENT)
			.setResponsePairID("pairId")
			.setTimestamp(123456000)
			.build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.UNKNOWN, event);
		AudienceHit audienceHit = hitQueue.queueParametersHit;
		assertEquals("url", audienceHit.url);
		assertEquals(5, audienceHit.timeout);
		assertEquals(123456, audienceHit.timestamp);
		assertEquals("pairId", audienceHit.pairId);
		assertEquals(20, audienceHit.eventNumber);

		assertFalse(hitQueue.bringOnlineWasCalled);
	}

	@Test
	public void testUpdatePrivacyStatus_When_OptIn() {
		audienceHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		assertTrue(hitQueue.bringOnlineWasCalled);
		assertFalse(hitQueue.suspendWasCalled);
		assertFalse(hitQueue.deleteAllHitsWasCalled);
	}

	@Test
	public void testUpdatePrivacyStatus_When_OptOut() {
		audienceHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		assertFalse(hitQueue.bringOnlineWasCalled);
		assertTrue(hitQueue.suspendWasCalled);
		assertTrue(hitQueue.deleteAllHitsWasCalled);
	}

	@Test
	public void testUpdatePrivacyStatus_When_OptUnknown() {
		audienceHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		assertFalse(hitQueue.bringOnlineWasCalled);
		assertTrue(hitQueue.suspendWasCalled);
		assertFalse(hitQueue.deleteAllHitsWasCalled);
	}

	private AudienceHit createHit(
		String identifier,
		long timeStamp,
		String url,
		String pairId,
		int eventNumber,
		int timeout
	) {
		AudienceHit newHit = new AudienceHit();
		newHit.identifier = identifier;
		newHit.eventNumber = eventNumber;
		newHit.pairId = pairId;
		newHit.url = url;
		newHit.timestamp = timeStamp;
		newHit.timeout = timeout;
		return newHit;
	}
}
