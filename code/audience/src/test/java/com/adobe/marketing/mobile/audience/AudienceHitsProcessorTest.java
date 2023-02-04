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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AudienceHitsProcessorTest {

	private static final Event mockAAMEvent = new Event.Builder(
		"test",
		EventType.AUDIENCEMANAGER,
		EventSource.REQUEST_CONTENT
	)
		.build();

	private AudienceHitProcessor audienceHitProcessor;

	@Mock
	private ServiceProvider mockServiceProvider;

	@Mock
	private Networking mockNetworkService;

	@Mock
	private AudienceNetworkResponseHandler mockNetworkResponseHandler;

	@Mock
	private HttpConnecting mockConnection;

	@Before
	public void setup() {
		ServiceProvider.getInstance().setNetworkService(mockNetworkService);
		audienceHitProcessor = new AudienceHitProcessor(mockNetworkResponseHandler);
	}

	@After
	public void tearDown() {
		reset(mockConnection);
		reset(mockNetworkResponseHandler);
		reset(mockNetworkService);
		reset(mockServiceProvider);
	}

	@Test
	public void testProcessHit_whenConnectionNull_doesNotRetry() {
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertTrue);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(null);

		ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockNetworkResponseHandler).complete(isNull(), requestEventCaptor.capture());
		assertEqualEvents(mockAAMEvent, requestEventCaptor.getValue());
	}

	@Test
	public void testProcessHit_whenResponseIsValid_doesNotRetry() {
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertTrue);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(mockConnection);

		ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockNetworkResponseHandler).complete(eq(""), requestEventCaptor.capture());
		assertEqualEvents(mockAAMEvent, requestEventCaptor.getValue());
		verify(mockConnection).close();
	}

	@Test
	public void testProcessHit_whenConnectionTimeout_retries() {
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertFalse);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(mockConnection);

		verify(mockNetworkResponseHandler, never()).complete(any(), any()); // response handler not called
		verify(mockConnection).close();
	}

	@Test
	public void testProcessHit_whenGatewayTimeout_retries() {
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertFalse);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(mockConnection);

		verify(mockNetworkResponseHandler, never()).complete(any(), any()); // response handler not called
		verify(mockConnection).close();
	}

	@Test
	public void testProcessHit_whenHttpUnavailable_retries() {
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertFalse);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(mockConnection);

		verify(mockNetworkResponseHandler, never()).complete(any(), any()); // response handler not called
		verify(mockConnection).close();
	}

	@Test
	public void testProcessHit_whenUnrecoverableResponseCode_doesNotRetry() {
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);
		when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertTrue);

		ArgumentCaptor<NetworkCallback> networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);
		verify(mockNetworkService).connectAsync(any(NetworkRequest.class), networkCallbackCaptor.capture());
		networkCallbackCaptor.getValue().call(mockConnection);

		ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockNetworkResponseHandler).complete(isNull(), requestEventCaptor.capture());
		assertEqualEvents(mockAAMEvent, requestEventCaptor.getValue());
		verify(mockConnection).close();
	}

	@Test
	public void testProcessHit_whenNullNetworkService_doesNotCrashAndRetries() {
		// mock ServiceProvider to return null NetworkService for this test
		MockedStatic<ServiceProvider> mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider.class);
		mockedStaticServiceProvider.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getNetworkService()).thenReturn(null);
		audienceHitProcessor = new AudienceHitProcessor(mockNetworkResponseHandler);
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);

		audienceHitProcessor.processHit(dataEntity.toDataEntity(), Assert::assertFalse);

		verify(mockNetworkService, never()).connectAsync(any(), any());
		verify(mockNetworkResponseHandler, never()).complete(any(), any()); // response handler not called

		mockedStaticServiceProvider.close();
	}

	@Test
	public void testProcessHit_whenInvalidDataEntity_doesNotCrashAndSkipsToNextHit() {
		audienceHitProcessor.processHit(new DataEntity("invalid AudienceDataEntity"), Assert::assertTrue);

		verify(mockNetworkService, never()).connectAsync(any(), any());
		verify(mockNetworkResponseHandler, never()).complete(any(), any()); // response handler not called
	}

	@Test
	public void testRetryAfter_returns30sec() {
		AudienceDataEntity dataEntity = new AudienceDataEntity(mockAAMEvent, "serverName2.com", 3);
		assertEquals(30, audienceHitProcessor.retryInterval(dataEntity.toDataEntity()));
	}

	private void assertEqualEvents(final Event expectedEvent, final Event actualEvent) {
		assertNotNull(expectedEvent);
		assertNotNull(actualEvent);
		assertEquals(expectedEvent.getName(), actualEvent.getName());
		assertEquals(expectedEvent.getType(), actualEvent.getType());
		assertEquals(expectedEvent.getSource(), actualEvent.getSource());
		assertEquals(expectedEvent.getUniqueIdentifier(), actualEvent.getUniqueIdentifier());
		assertEquals(expectedEvent.getTimestamp(), actualEvent.getTimestamp());
		assertEquals(expectedEvent.getEventData(), actualEvent.getEventData());
	}
}
