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

import static com.adobe.marketing.mobile.audience.AudienceTestConstants.LOG_TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestableNetworkService implements Networking {

	private static final String LOG_SOURCE = "FunctionalTestNetworkService";
	private final Map<TestableNetworkRequest, List<TestableNetworkRequest>> receivedTestableNetworkRequests;
	private final Map<TestableNetworkRequest, HttpConnecting> responseMatchers;
	private final Map<TestableNetworkRequest, ADBCountDownLatch> expectedTestableNetworkRequests;
	private final ExecutorService executorService; // simulating the async network service
	private Integer delayedResponse = 0;

	public TestableNetworkService() {
		receivedTestableNetworkRequests = new HashMap<>();
		responseMatchers = new HashMap<>();
		expectedTestableNetworkRequests = new HashMap<>();
		executorService = Executors.newCachedThreadPool();
	}

	private static final HttpConnecting defaultResponse = new HttpConnecting() {
		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream("".getBytes());
		}

		@Override
		public InputStream getErrorStream() {
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public String getResponseMessage() {
			return "";
		}

		@Override
		public String getResponsePropertyValue(String responsePropertyKey) {
			return null;
		}

		@Override
		public void close() {}
	};

	public void reset() {
		Log.trace(LOG_TAG, LOG_SOURCE, "Reset received and expected network requests.");
		receivedTestableNetworkRequests.clear();
		responseMatchers.clear();
		expectedTestableNetworkRequests.clear();
		delayedResponse = 0;
	}

	public void setResponseConnectionFor(
		final TestableNetworkRequest request,
		final HttpConnecting responseConnection
	) {
		responseMatchers.put(request, responseConnection);
	}

	public void setExpectedNetworkRequest(final TestableNetworkRequest request, final int count) {
		expectedTestableNetworkRequests.put(request, new ADBCountDownLatch(count));
	}

	public Map<TestableNetworkRequest, ADBCountDownLatch> getExpectedNetworkRequests() {
		return expectedTestableNetworkRequests;
	}

	public List<TestableNetworkRequest> getReceivedNetworkRequestsMatching(final TestableNetworkRequest request) {
		for (Map.Entry<TestableNetworkRequest, List<TestableNetworkRequest>> requests : receivedTestableNetworkRequests.entrySet()) {
			if (requests.getKey().equals(request)) {
				return requests.getValue();
			}
		}

		return Collections.emptyList();
	}

	public boolean isNetworkRequestExpected(final TestableNetworkRequest request) {
		return expectedTestableNetworkRequests.containsKey(request);
	}

	public boolean awaitFor(final TestableNetworkRequest request, final int timeoutMillis) throws InterruptedException {
		for (Map.Entry<TestableNetworkRequest, ADBCountDownLatch> expected : expectedTestableNetworkRequests.entrySet()) {
			if (expected.getKey().equals(request)) {
				return expected.getValue().await(timeoutMillis, TimeUnit.MILLISECONDS);
			}
		}

		return true;
	}

	public void enableDelayedResponse(final Integer delaySec) {
		if (delaySec < 0) {
			return;
		}

		delayedResponse = delaySec;
	}

	/**
	 * Asserts that the correct number of network requests were being sent, based on the previously set expectations.
	 * @throws InterruptedException
	 * @see #setExpectedNetworkRequest(TestableNetworkRequest, int)
	 */
	public void assertNetworkRequestCount() throws InterruptedException {
		Map<TestableNetworkRequest, ADBCountDownLatch> expectedNetworkRequests = getExpectedNetworkRequests();

		if (expectedNetworkRequests.isEmpty()) {
			fail(
				"There are no network request expectations set, use this API after calling setExpectationNetworkRequest"
			);
			return;
		}

		for (Map.Entry<TestableNetworkRequest, ADBCountDownLatch> expectedRequest : expectedNetworkRequests.entrySet()) {
			boolean awaitResult = expectedRequest.getValue().await(5, TimeUnit.SECONDS);
			assertTrue(
				"Time out waiting for network request with URL '" +
				expectedRequest.getKey().getUrl() +
				"' and method '" +
				expectedRequest.getKey().getMethod().name() +
				"'",
				awaitResult
			);
			int expectedCount = expectedRequest.getValue().getInitialCount();
			int receivedCount = expectedRequest.getValue().getCurrentCount();
			String message = String.format(
				"Expected %d network requests for URL %s (%s), but received %d",
				expectedCount,
				expectedRequest.getKey().getUrl(),
				expectedRequest.getKey().getMethod(),
				receivedCount
			);
			assertEquals(message, expectedCount, receivedCount);
		}
	}

	@Override
	public void connectAsync(NetworkRequest networkRequest, NetworkCallback resultCallback) {
		Log.trace(
			LOG_TAG,
			LOG_SOURCE,
			"Received connectUrlAsync to URL '%s' and HttpMethod '%s'.",
			networkRequest.getUrl(),
			networkRequest.getMethod().name()
		);

		executorService.submit(() -> {
			HttpConnecting response = setNetworkRequest(
				new TestableNetworkRequest(
					networkRequest.getUrl(),
					networkRequest.getMethod(),
					networkRequest.getBody(),
					networkRequest.getHeaders(),
					networkRequest.getConnectTimeout(),
					networkRequest.getReadTimeout()
				)
			);

			if (resultCallback != null) {
				if (delayedResponse > 0) {
					try {
						Thread.sleep(delayedResponse * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				resultCallback.call(response == null ? defaultResponse : response);
			}
		});
	}

	/**
	 * Add the network request to the list of received requests. Returns the matching response, or
	 * the default response if no matching response was found.
	 */
	private HttpConnecting setNetworkRequest(TestableNetworkRequest networkRequest) {
		if (!receivedTestableNetworkRequests.containsKey(networkRequest)) {
			receivedTestableNetworkRequests.put(networkRequest, new ArrayList<>());
		}

		receivedTestableNetworkRequests.get(networkRequest).add(networkRequest);

		HttpConnecting response = getMatchedResponse(networkRequest);
		countDownExpected(networkRequest);

		return response == null ? defaultResponse : response;
	}

	private void countDownExpected(final TestableNetworkRequest request) {
		for (Map.Entry<TestableNetworkRequest, ADBCountDownLatch> expected : expectedTestableNetworkRequests.entrySet()) {
			if (expected.getKey().equals(request)) {
				expected.getValue().countDown();
			}
		}
	}

	private HttpConnecting getMatchedResponse(final TestableNetworkRequest request) {
		for (Map.Entry<TestableNetworkRequest, HttpConnecting> responses : responseMatchers.entrySet()) {
			if (responses.getKey().equals(request)) {
				return responses.getValue();
			}
		}

		return null;
	}
}
