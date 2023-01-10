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
 * Listens for {@code AUDIENCEMANAGER}, {@code REQUEST_RESET} events and invokes reset method on
 * the parent {@code Audience} module.
 */
class ListenerAudienceRequestResetAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerAudienceRequestResetAudienceManager.class.getSimpleName();
	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	ListenerAudienceRequestResetAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to event {@code AUDIENCEMANAGER}, {@code REQUEST_RESET} event and calls the method in the parent
	 * {@code Audience} Module to reset in-memory and persisted AAM variables.
	 *
	 * @param event	incoming {@link Event}
	 */
	@Override
	public void hear(final Event event) {
		// let parent module know it should reset its state
		Log.trace(LOG_TAG, "hear - resetting state");
		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				parentModule.reset(event);
			}
		});

	}
}
