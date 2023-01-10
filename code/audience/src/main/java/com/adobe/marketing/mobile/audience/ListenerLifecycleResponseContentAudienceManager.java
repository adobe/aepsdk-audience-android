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
 * Listens for {@code LIFECYCLE}, {@code RESPONSE_CONTENT} events and queues them for
 * processing by the parent {@code Audience} module.
 */
class ListenerLifecycleResponseContentAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerLifecycleResponseContentAudienceManager.class.getSimpleName();
	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	public ListenerLifecycleResponseContentAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code EventType#LIFECYCLE}, {@code EventSource#RESPONSE_CONTENT} event.
	 * <p>
	 * Converts event data to something consumable by the {@link AudienceExtension} module. The {@code Audience} module then processes
	 * the lifecycle data and sends hit to AAM server if {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#ANALYTICS_CONFIG_AAMFORWARDING}
	 * is disabled.
	 *
	 * @param event	incoming {@link Event}
	 */
	public void hear(final Event event) {
		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				final EventData eventData = event.getData();

				// if we don't have valid data in our event, don't queue it
				if (eventData == null || !eventData.containsKey(AudienceConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA)) {
					Log.warning(LOG_TAG, "hear - Ignoring Lifecycle response as event data unavailable.");
					return;
				}

				Log.trace(LOG_TAG, "hear - queueing the event as we have event data and valid context data.");
				parentModule.queueAamEvent(event);
			}
		});
	}
}