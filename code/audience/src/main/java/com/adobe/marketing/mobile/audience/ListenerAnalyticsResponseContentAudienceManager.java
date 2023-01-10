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

/**
 * Listens for {@code ANALYTICS}, {@code RESPONSE_CONTENT} events and passes them to
 * the parent {@code Audience} module for processing.
 */
class ListenerAnalyticsResponseContentAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerAnalyticsResponseContentAudienceManager.class.getSimpleName();

	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	public ListenerAnalyticsResponseContentAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code ANALYTICS}, {@code RESPONSE_CONTENT} event, processes it and saves the visitor profile.
	 * <p>
	 * {@code Analytics} Module will dispatch this event if {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#ANALYTICS_CONFIG_AAMFORWARDING} is enabled.
	 *
	 * @param event	incoming {@link Event}
	 */
	public void hear(final Event event) {

		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				EventData eventData = event.getData();

				if (eventData == null
						|| !eventData.containsKey(AudienceConstants.EventDataKeys.Analytics.ANALYTICS_SERVER_RESPONSE_KEY)) {
					Log.warning(LOG_TAG,
								"hear - Ignoring Analytics response as event data or analytics server response key is unavailable.");
					return;
				}

				String analyticsResponse = eventData.optString(AudienceConstants.EventDataKeys.Analytics.ANALYTICS_SERVER_RESPONSE_KEY,
										   null);

				if (!StringUtils.isNullOrEmpty(analyticsResponse)) {
					Log.trace(LOG_TAG, "hear - Processing Analytics response.");
					parentModule.processResponse(analyticsResponse, event);
				}
			}
		});

	}

}