/* ***********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2018 Adobe Systems Incorporated
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
 **************************************************************************/

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceConstants.LOG_TAG;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.HitProcessing;
import com.adobe.marketing.mobile.services.HitProcessingResult;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkingConstants;
import com.adobe.marketing.mobile.util.StreamUtils;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AudienceHitProcessor is responsible for processing hits retrieved from the {@code Audience} hit queue
 */
class AudienceHitProcessor implements HitProcessing {
	private static final String LOG_SOURCE = "AudienceHitProcessor";
	private static final String AAM_DATABASE_FILENAME           = "ADBMobileAAM.sqlite";
	private static final int HIT_QUEUE_RETRY_TIME_SECONDS = 30;

	private final Networking networkService;
	private AudienceExtension parentModule;

	AudienceHitProcessor(final Networking networkService) {
		this.networkService = networkService;

		// todo: revisit me
		// if this is a new session, the known event number and pair id values have no use, so we should reset them
		resetEventNumberAndPairIdForExistingRequests();
	}

	@Override
	public int retryInterval(@NonNull DataEntity dataEntity) {
		return HIT_QUEUE_RETRY_TIME_SECONDS;
	}

	@Override
	public void processHit(@NonNull DataEntity dataEntity, @NonNull HitProcessingResult processingResult) {
		AudienceDataEntity entity = AudienceDataEntity.fromDataEntity(dataEntity);

		if (entity == null) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to deserialize DataEntity to AudienceDataEntity. Dropping the hit.");
			processingResult.complete(true);
			return;
		}

		Log.trace(LOG_TAG, LOG_SOURCE, "Processing hit request: %s", entity.getUrl());

		final Event aamEvent = entity.getEvent();

		// TODO: implement me
//		parentModule.getExecutor().execute(new Runnable() {
//			@Override
//			public void run() {
//				// prepare shared state for this asynchronous processing of this event
//				parentModule.createSharedState(aamEvent.getEventNumber(), EventHub.SHARED_STATE_PENDING);
//			}
//		});

		final NetworkRequest networkRequest = new NetworkRequest(entity.getUrl(), HttpMethod.GET, null, null, entity.getTimeoutSec(), entity.getTimeoutSec());
		if (networkService == null) {
			Log.warning(LOG_TAG, LOG_SOURCE, "Unexpected null NetworkService, unable to execute the request at this time.");
			processingResult.complete(false);
			return;
		}

		networkService.connectAsync(networkRequest, connection -> {
			// a null connection represents an invalid request
			if (connection == null) {
				Log.warning(LOG_TAG, LOG_SOURCE, "AAM could not process a request because it was invalid, discarding hit.");

				// make sure the parent updates shared state and notifies one-time listeners accordingly
				parentModule.handleNetworkResponse(null, aamEvent); //todo: update all usages of parentModule.handleNetworkResponse

				processingResult.complete(true);
				return;
			}

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				final String response = StreamUtils.readAsString(connection.getInputStream());

				// pass the response back to parent, delete hit
				parentModule.handleNetworkResponse(response, aamEvent);
				processingResult.complete(true);
			} else if (!NetworkingConstants.RECOVERABLE_ERROR_CODES.contains(connection.getResponseCode())) {
				// unrecoverable error. delete the hit from the database and continue
				Log.warning(LOG_TAG, LOG_SOURCE, "Unrecoverable network error code (%d) while processing AAM requests, discarding hit.", connection.getResponseCode());

				// make sure the parent updates shared state and notifies one-time listeners accordingly
				parentModule.handleNetworkResponse(null, aamEvent);

				// delete the current request and move on to the next
				processingResult.complete(true);
			} else {
				processingResult.complete(false); // recoverable error code, will retry
			}
		});
	}

	/**
	 * Creates a new record in the {@code Audience} database.
	 * <p>
	 * If the insert fails, a warning will be logged and the database will be deleted and re-created.
	 *
	 * @param url {@link String} containing URL of Audience Manager request
	 * @param timeout {@code int} indicating timeout value
	 * @param privacyStatus the current privacy status of the SDK
	 * @param event {@link Event} instance containing information for this request
	 */
	void queue(final String url, final int timeout, final MobilePrivacyStatus privacyStatus, final Event event) {
		if (event == null) {
			Log.warning(LOG_TAG, "queue - Not queuing hits as the request event is empty");
			return;
		}

		if (StringUtils.isNullOrEmpty(url)) {
			Log.warning(LOG_TAG, "queue - Not queuing hits as the request url is empty");
			return;
		}

		// kick the work over to the runnable task on the aam executor thread
		AudienceDataEntity audienceHit = new AudienceDataEntity();
		audienceHit.url = url;
		audienceHit.timeout = timeout;
		audienceHit.timestamp = TimeUnit.MILLISECONDS.toSeconds(event.getTimestamp());
		audienceHit.pairId = event.getResponsePairID();
		audienceHit.eventNumber = event.getEventNumber();
		this.hitQueue.queue(audienceHit);
		Log.trace(LOG_TAG, "queue - Successfully queued hit");

		// send the aam hit only when privacy status is opted-in
		if (privacyStatus == MobilePrivacyStatus.OPT_IN) {
			Log.trace(LOG_TAG, "queue - Trying to send hit as privacy is opted-in");
			this.hitQueue.bringOnline();
		}
	}

	// ================================================================================================================
	// private methods & classes
	// ================================================================================================================

	/**
	 * The event hub does not live cross-session, so {@code pairId} and {@code event number} have no use if they exist outside of
	 * the session in which they originated.
	 * <p>
	 * This method should only be called when the {@link AudienceHitProcessor} initializes. It sets {@code event number} and
	 * {@code pairId} to -1, invalidating them and alerting the {@link AudienceExtension} that it need not attempt to process a
	 * response event or update shared state for this request.
	 */
	private void resetEventNumberAndPairIdForExistingRequests() {
		Map<String, Object>  updateValues = this.audienceHitSchema.generateUpdateValuesForResetEventNumberAndPairId();
		this.hitQueue.updateAllHits(updateValues);
	}
}
