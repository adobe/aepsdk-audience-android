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
 * Listens for {@code CONFIGURATION}, {@code RESPONSE_CONTENT} events and invokes method on
 * the parent {@code Audience} module to update mobile privacy status.
 */
class ListenerConfigurationResponseContentAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerConfigurationResponseContentAudienceManager.class.getSimpleName();
	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	ListenerConfigurationResponseContentAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code EventType#CONFIGURATION}, {@code EventSource#RESPONSE_CONTENT} event.
	 * <p>
	 * Invokes {@link AudienceExtension#processConfiguration(Event)} to process the event.
	 *
	 * @param event incoming {@link Event}
	 */
	@Override
	public void hear(final Event event) {
		if (event.getData() != null) {
			Log.trace(LOG_TAG, "hear - Processing Configuration response.");
			parentModule.getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					parentModule.processConfiguration(event);
				}
			});
		}
	}
}
