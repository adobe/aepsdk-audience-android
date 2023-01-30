/*
  Copyright 2018 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceConstants.LOG_TAG;

import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.HitProcessing;
import com.adobe.marketing.mobile.services.HitProcessingResult;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkingConstants;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StreamUtils;
import java.net.HttpURLConnection;

/**
 * AudienceHitProcessor is responsible for processing hits retrieved from the {@code Audience} hit queue
 */
class AudienceHitProcessor implements HitProcessing {

	private static final String LOG_SOURCE = "AudienceHitProcessor";
	private static final int HIT_QUEUE_RETRY_TIME_SECONDS = 30;

	private final Networking networkService;
	private final AudienceNetworkResponseHandler networkResponseHandler;

	AudienceHitProcessor(final AudienceNetworkResponseHandler networkResponseHandler) {
		this.networkService = ServiceProvider.getInstance().getNetworkService();
		this.networkResponseHandler = networkResponseHandler;
	}

	@Override
	public int retryInterval(@NonNull DataEntity dataEntity) {
		return HIT_QUEUE_RETRY_TIME_SECONDS;
	}

	@Override
	public void processHit(@NonNull DataEntity dataEntity, @NonNull HitProcessingResult processingResult) {
		if (networkService == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Unexpected null NetworkService, unable to execute the request at this time."
			);
			processingResult.complete(false);
			return;
		}

		AudienceDataEntity entity = AudienceDataEntity.fromDataEntity(dataEntity);

		if (entity == null) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to deserialize DataEntity to AudienceDataEntity, discarding hit.");
			processingResult.complete(true);
			return;
		}

		Log.trace(LOG_TAG, LOG_SOURCE, "Processing hit request: %s", entity.getUrl());

		final NetworkRequest networkRequest = new NetworkRequest(
			entity.getUrl(),
			HttpMethod.GET,
			null,
			null,
			entity.getTimeoutSec(),
			entity.getTimeoutSec()
		);

		networkService.connectAsync(
			networkRequest,
			connection -> handleNetworkResponse(connection, entity.getEvent(), processingResult)
		);
	}

	/**
	 * Handles the network response after a hit has been sent to the server
	 *
	 * @param connection the connection object returned for the network request
	 * @param requestEvent the request event that initiated this network call
	 * @param processingResult a callback to be invoked after processing the response, with true for success and false for failure (retry)
	 */
	private void handleNetworkResponse(
		final HttpConnecting connection,
		final Event requestEvent,
		@NonNull HitProcessingResult processingResult
	) {
		// a null connection represents an invalid request
		if (connection == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"AAM could not process response connection because it was null, discarding hit."
			);

			// calls handler to update the shared state and notifies listeners accordingly
			networkResponseHandler.complete(null, requestEvent);
			processingResult.complete(true);
			return;
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			final String response = StreamUtils.readAsString(connection.getInputStream());

			// pass the response back to handler, delete hit
			networkResponseHandler.complete(response, requestEvent);
			processingResult.complete(true);
		} else if (!NetworkingConstants.RECOVERABLE_ERROR_CODES.contains(connection.getResponseCode())) {
			// unrecoverable error. delete the hit from the database and continue
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Unrecoverable network error code (%d) while processing AAM requests, discarding hit.",
				connection.getResponseCode()
			);

			// calls handler to update the shared state and notifies listeners accordingly
			networkResponseHandler.complete(null, requestEvent);

			// delete the current request and move on to the next
			processingResult.complete(true);
		} else {
			processingResult.complete(false); // recoverable error code, will retry later
		}
	}
}
