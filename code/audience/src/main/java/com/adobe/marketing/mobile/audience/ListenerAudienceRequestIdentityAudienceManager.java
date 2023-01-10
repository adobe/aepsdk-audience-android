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
 * Listens for {@code IDENTITY}, {@code REQUEST_IDENTITY} events and invokes appropriate
 * parent {@code Audience} module methods for handling.
 */
class ListenerAudienceRequestIdentityAudienceManager extends ModuleEventListener<AudienceExtension> {
	private static final String LOG_TAG = ListenerAudienceRequestIdentityAudienceManager.class.getSimpleName();
	/**
	 * Constructor
	 *
	 * @param extension {@link AudienceExtension} that owns this listener
	 * @param type {@link EventType} that this listener will hear
	 * @param source {@link EventSource} that this listener will hear
	 */
	ListenerAudienceRequestIdentityAudienceManager(final AudienceExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code IDENTITY}, {@code REQUEST_IDENTITY} event and takes appropriate action based on {@code EventData} keys.
	 * <ul>
	 *     <li>If {@link EventData} object contains {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Audience#DPID} and
	 *     {@link com.adobe.marketing.mobile.AudienceConstants.EventDataKeys.Audience#DPUUID} keys, calls {@link AudienceExtension#setDpidAndDpuuid(String, String, Event)}
	 *     to set DPID and DPUUID.</li>
	 *     <li>If the above is not true, retrieves AAM variables such as DPID, DPUUID and visitor profile from the parent module with a
	 *     call to {@link AudienceExtension#getIdentityVariables(String)}.</li>
	 * </ul>
	 *
	 * @param event	incoming {@link Event}
	 */
	@Override
	public void hear(final Event event) {
		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				final EventData eventData = event.getData();

				// if the event data for the request contains dpid and dpuuid, call the setter
				if (eventData != null && eventData.containsKey(AudienceConstants.EventDataKeys.Audience.DPID) &&
						eventData.containsKey(AudienceConstants.EventDataKeys.Audience.DPUUID)) {
					parentModule.setDpidAndDpuuid(eventData.optString(AudienceConstants.EventDataKeys.Audience.DPID, null),
												  eventData.optString(AudienceConstants.EventDataKeys.Audience.DPUUID, null), event);
					Log.trace(LOG_TAG, "hear - Set dpid and dpuuid. Dpid and dpuuid are present");
				}
				// else (if the dpid and dpuuid values are not present), call the getter and pass along the pairId
				else {
					parentModule.getIdentityVariables(event.getResponsePairID());
					Log.trace(LOG_TAG, "hear - Call the getter and pass along the pairid. Dpid and dpuuid are not present");
				}
			}
		});


	}
}
