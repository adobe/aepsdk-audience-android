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
 * Listens for {@code AUDIENCEMANAGER}, {@code REQUEST_CONTENT} events and queues them for
 * processing by the parent {@code Audience} module.
 */
class ListenerAudienceRequestContentAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerAudienceRequestContentAudienceManager.class.getSimpleName();

	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	public ListenerAudienceRequestContentAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code AUDIENCEMANAGER}, {@code REQUEST_CONTENT} event and queues it for processing when
	 * the necessary module shared states are available.
	 * <p>
	 * This event will result in a call to {@link AudienceExtension#submitSignal(Event)} to submit signal to Audience Manager
	 * if {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Configuration#AAM_CONFIG_SERVER} is configured.
	 *
	 * @param event incoming {@link Event}
	 */
	@Override
	public void hear(final Event event) {
		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				// add the event to the queue
				Log.trace(LOG_TAG, "hear - Processing Audience request.");
				parentModule.queueAamEvent(event);
			}
		});
	}
}
