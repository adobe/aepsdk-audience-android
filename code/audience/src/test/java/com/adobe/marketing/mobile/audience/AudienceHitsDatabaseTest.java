/**
 * **********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 * <p>
 * Copyright 2017 Adobe Systems Incorporated
 * All Rights Reserved.
 * <p>
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

package com.adobe.marketing.mobile.audience;

import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;


public class AudienceHitsDatabaseTest extends BaseTest {

	private AudienceHitProcessor audienceHitsDatabase;
	private MockAudienceExtension parentModule;
	private MockNetworkService networkService;
	private MockHitQueue<AudienceDataEntity, AudienceHitSchema> hitQueue;

	@Before
	public void setup() {
		super.beforeEach();
		hitQueue = new MockHitQueue<AudienceDataEntity, AudienceHitSchema>(platformServices);
		parentModule = new MockAudienceExtension(eventHub, platformServices);
		networkService = platformServices.getMockNetworkService();
		audienceHitsDatabase = new AudienceHitProcessor(parentModule, platformServices, hitQueue);
	}

	@Test
	public void testProcess_NotRetry_When_ConnectionIsNull() throws Exception {
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
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
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", 200, null,
				null);
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
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_CLIENT_TIMEOUT, null,
				null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}

	@Test
	public void testProcess_Retry_When_GateWayOout() throws Exception {
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_GATEWAY_TIMEOUT, null,
				null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}


	@Test
	public void testProcess_Retry_When_HttpUnavailable() throws Exception {
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_UNAVAILABLE, null,
				null);
		networkService.connectUrlReturnValue = mockConnection;

		HitQueue.RetryType retryType = audienceHitsDatabase.process(audienceHit);
		assertEquals(HitQueue.RetryType.YES, retryType);
		assertFalse(parentModule.handleNetworkResponseWasCalled);
	}

	@Test
	public void testProcess_NotRetry_When_OtherRepsonseCode() throws Exception {
		AudienceDataEntity audienceHit = createHit("id", 3000, "serverName2.com", "pairId", 3, 5);
		MockConnection mockConnection = new MockConnection("", 301, null,
				null);
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
		.setResponsePairID("pairId").setTimestamp(123456000).build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.OPT_IN, event);
		AudienceDataEntity audienceHit = hitQueue.queueParametersHit;
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
		.setResponsePairID("pairId").setTimestamp(123456000).build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.OPT_OUT, event);
		AudienceDataEntity audienceHit = hitQueue.queueParametersHit;
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
		.setResponsePairID("pairId").setTimestamp(123456000).build();
		event.setEventNumber(20);

		audienceHitsDatabase.queue("url", 5, MobilePrivacyStatus.UNKNOWN, event);
		AudienceDataEntity audienceHit = hitQueue.queueParametersHit;
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

	private AudienceDataEntity createHit(String identifier, long timeStamp, String url, String pairId,
										 int eventNumber, int timeout) {
		AudienceDataEntity newHit = new AudienceDataEntity();
		newHit.identifier = identifier;
		newHit.eventNumber = eventNumber;
		newHit.pairId = pairId;
		newHit.url = url;
		newHit.timestamp = timeStamp;
		newHit.timeout = timeout;
		return newHit;
	}
}