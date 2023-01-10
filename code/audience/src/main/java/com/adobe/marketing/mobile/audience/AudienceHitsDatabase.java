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

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AudienceHitsDatabase class is:
 *<ol>
 *     <li>Owner of the database used to queue Audience Manager request.</li>
 *     <li>Responsible for processing hits retrieved from the {@code Audience} {@link HitQueue}</li>
 *</ol>
 */
class AudienceHitsDatabase implements HitQueue.IHitProcessor<AudienceHit> {
	// class members
	private static final String LOG_TAG = AudienceHitsDatabase.class.getSimpleName();

	// database constants
	private static final String AAM_DATABASE_FILENAME           = "ADBMobileAAM.sqlite";
	private static final String TABLE_REQUESTS                  = "REQUESTS";

	private final NetworkService networkService;
	private final AudienceExtension parentModule;
	private final HitQueue<AudienceHit, AudienceHitSchema> hitQueue;
	private final AudienceHitSchema audienceHitSchema;

	/**
	 * Constructor
	 *
	 * @param parent instance of {@link AudienceExtension} that owns this database
	 * @param services {@link PlatformServices} instance needed to create a database instance
	 * @param hitQueue used for unit test where a {@link HitQueue} will be passed in
	 */
	AudienceHitsDatabase(final AudienceExtension parent, final PlatformServices services,
						 final HitQueue<AudienceHit, AudienceHitSchema> hitQueue) {
		this.audienceHitSchema = new AudienceHitSchema();

		final File directory = services.getSystemInfoService() != null ?
							   services.getSystemInfoService().getApplicationCacheDir() : null;
		final File dbFilePath = new File(directory, AAM_DATABASE_FILENAME);

		if (hitQueue != null) {
			this.hitQueue = hitQueue;
		} else {
			this.hitQueue = new HitQueue<AudienceHit, AudienceHitSchema>(services, dbFilePath,
					TABLE_REQUESTS, audienceHitSchema, this);
		}

		this.parentModule = parent;
		this.networkService = services.getNetworkService();

		// if this is a new session, the known event number and pair id values have no use, so we should reset them
		resetEventNumberAndPairIdForExistingRequests();
	}

	/**
	 * Constructor
	 *
	 * @param parent instance of {@link AudienceExtension} that owns this database
	 * @param services {@link PlatformServices} instance needed to create a database instance
	 */
	AudienceHitsDatabase(final AudienceExtension parent, final PlatformServices services) {
		this(parent, services, null);
	}

	/**
	 * Processes the {@code AudienceHit}.
	 *
	 * @param hit {@code AudienceHit} to be processed
	 * @return {@link HitQueue.RetryType#NO} if the processed hit should be removed from the database
	 */
	@Override
	public HitQueue.RetryType process(final AudienceHit hit) {
		Log.trace(LOG_TAG, "process - Sending request (%s)", hit.url);

		final Event aamEvent = hit.getEvent();

		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				// prepare shared state for this asynchronous processing of this event
				parentModule.createSharedState(aamEvent.getEventNumber(), EventHub.SHARED_STATE_PENDING);
			}
		});


		// make the request synchronously
		final NetworkService.HttpConnection connection = networkService.connectUrl(hit.url,
				NetworkService.HttpCommand.GET, null, null, hit.timeout, hit.timeout);

		// a null connection represents an invalid request
		if (connection == null) {
			Log.warning(LOG_TAG, "process -  Discarding request. AAM could not process a request because it was invalid.");

			// make sure the parent updates shared state and notifies one-time listeners accordingly
			parentModule.handleNetworkResponse(null, aamEvent);

			return HitQueue.RetryType.NO;
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			final String response = StringUtils.streamToString(connection.getInputStream());

			// pass the response back to parent, delete hit
			parentModule.handleNetworkResponse(response, aamEvent);
			return HitQueue.RetryType.NO;
		} else if (!NetworkConnectionUtil.recoverableNetworkErrorCodes.contains(connection.getResponseCode())) {
			// unrecoverable error. delete the hit from the database and continue
			Log.warning(LOG_TAG, "process - Discarding request. Un-recoverable network error while processing AAM requests.");

			// make sure the parent updates shared state and notifies one-time listeners accordingly
			parentModule.handleNetworkResponse(null, aamEvent);

			// delete the current request and move on to the next
			return HitQueue.RetryType.NO;
		} else {
			return HitQueue.RetryType.YES;
		}
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
		AudienceHit audienceHit = new AudienceHit();
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

	/**
	 * Updates the mobile privacy status and takes one of the following actions based on privacy status setting.
	 * <ul>
	 *     <li>If {@link MobilePrivacyStatus#OPT_IN}, resumes processing the queued hits.</li>
	 *     <li>If {@link MobilePrivacyStatus#OPT_OUT}, suspends the queue and deletes all hits.</li>
	 *     <li>If {@link MobilePrivacyStatus#UNKNOWN}, suspends the queue.</li>
	 * </ul>
	 *
	 * @param privacyStatus {@link MobilePrivacyStatus} the new privacy status
	 */
	void updatePrivacyStatus(final MobilePrivacyStatus privacyStatus) {
		switch (privacyStatus) {
			case OPT_IN:
				Log.debug(LOG_TAG, "updatePrivacyStatus - Privacy opted-in: Attempting to send AAM queued hits from database");
				this.hitQueue.bringOnline();
				break;

			case OPT_OUT:
				this.hitQueue.suspend();
				Log.debug(LOG_TAG, "updatePrivacyStatus - Privacy opted-out: Clearing AAM queued hits from database");
				this.hitQueue.deleteAllHits();
				break;

			case UNKNOWN:
				this.hitQueue.suspend();
				Log.debug(LOG_TAG, "updatePrivacyStatus - Privacy opt-unknown: Suspend Audience database");
				break;
		}
	}

	// ================================================================================================================
	// private methods & classes
	// ================================================================================================================

	/**
	 * The event hub does not live cross-session, so {@code pairId} and {@code event number} have no use if they exist outside of
	 * the session in which they originated.
	 * <p>
	 * This method should only be called when the {@link AudienceHitsDatabase} initializes. It sets {@code event number} and
	 * {@code pairId} to -1, invalidating them and alerting the {@link AudienceExtension} that it need not attempt to process a
	 * response event or update shared state for this request.
	 */
	private void resetEventNumberAndPairIdForExistingRequests() {
		Map<String, Object>  updateValues = this.audienceHitSchema.generateUpdateValuesForResetEventNumberAndPairId();
		this.hitQueue.updateAllHits(updateValues);
	}

}
