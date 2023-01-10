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
 * Listens for {@code HUB}, {@code SHARED_STATE} events and passes them to the parent
 * {@code Audience} module to kick off processing.
 */
class ListenerHubSharedStateAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerHubSharedStateAudienceManager.class.getSimpleName();
	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	public ListenerHubSharedStateAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code EventType#HUB}, {@code EventSource#SHARED_STATE} event.
	 * <p>
	 * If there is an event owner named, passes the owner's name to the parent {@link AudienceExtension} module to
	 * potentially kick off processing.
	 *
	 * @param event incoming {@link Event}
	 */
	@Override
	public void hear(final Event event) {
		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				final EventData eventData = event.getData();

				if (eventData == null || eventData.isEmpty()) {
					Log.warning(LOG_TAG, "hear - Ignoring shared state change as event data is unavailable.");
					return;
				}

				// get name of event state that changed from event data & tell the parent module to process it
				final String eventStateName = eventData.optString(AudienceConstants.EventDataKeys.STATE_OWNER, null);

				if (!StringUtils.isNullOrEmpty(eventStateName)) {
					Log.trace(LOG_TAG, "hear - Processing shared state change.");
					parentModule.processStateChange(eventStateName);
				}
			}
		});
	}
}
