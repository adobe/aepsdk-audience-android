/* ************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2017 Adobe Systems Incorporated
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

import java.util.HashMap;
import java.util.Map;

class AudienceCore {

	private static final String LOG_TAG = AudienceCore.class.getSimpleName();

	EventHub eventHub;

	AudienceCore(final EventHub eventHub, final ModuleDetails moduleDetails) {
		this(eventHub, moduleDetails, true);
	}

	AudienceCore(final EventHub eventHub, final ModuleDetails moduleDetails, final boolean registerExtension) {
		if (eventHub == null) {
			Log.error(LOG_TAG, "AudienceCore - Core initialization was unsuccessful (No EventHub instance found!)");
			return;
		}

		this.eventHub = eventHub;

		if (registerExtension) {

			Class audienceClass = AudienceExtension.class;

			try {
				eventHub.registerModule(audienceClass, moduleDetails);
				Log.debug(LOG_TAG, "Registered %s extension", AudienceExtension.class.getSimpleName());
			} catch (InvalidModuleException e) {
				Log.debug(LOG_TAG, "AudienceCore - Failed to register %s module (%s)", audienceClass.getSimpleName(), e);
			}
		}

		Log.debug(LOG_TAG, "AudienceCore - Core initialization was successful");
	}

	/**
	 * Initiates an Audience Manager Content Request event
	 *
	 * @param data     (optional) traits dictionary to be attributed to the user's AAM profile
	 * @param callback (optional) {@link AdobeCallback} instance which is invoked with AAM Visitor Profile map as a parameter
	 * @param callback (optional) {@link AdobeCallbackWithError} instance which is invoked with AAM Visitor Profile map as a parameter
	 *  	                     or an error if there is an error with the underlying call or if it times out.
	 *
	 */
	void submitAudienceProfileData(final Map<String, String> data,
								   final AdobeCallback<Map<String, String>> callback) {

	}


	/**
	 * Returns the current Data Provider ID (DPID) and the current Data Provider Unique User ID (DPUUID).
	 * <p>
	 * Data Provider ID, also referred to as Data Source ID is assigned to each data source in
	 * Audience Manager. If DPID is not set null is returned.
	 *
	 * Data Provider Unique User ID, also referred to as CRM ID is the data provider's unique ID
	 * for the user in Audience Manager CRM system. DPUUIDs can be synced with Audience Manager UUIDs.
	 * If DPUUID is not set, null is returned.
	 *
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the Map of DPID and DPUUID  as a parameter
	 * @see AdobeCallback
	 */
	void getDpidAndDpuuid(final AdobeCallback<Map<String, String>> adobeCallback) {
		identityRequest(AudienceConstants.EventDataKeys.Audience.AUDIENCE_IDS, adobeCallback);
	}

	/**
	 * Returns the visitor profile that was most recently obtained.
	 * <p>
	 * Visitor profile is saved in SharedPreference for easy access across multiple launches of your app.
	 * If no audience signal has been submitted yet, null is returned.
	 *
	 *
	 * @param adobeCallback {@link AdobeCallback} instance which is invoked with the AAM Visitor Profile map as a parameter
	 * @see AdobeCallback
	 * @param adobeCallback {@link AdobeCallbackWithError} instance which is invoked with AAM Visitor Profile map
	 * 	                     or an error if there is an error with the underlying call or if it times out.
	 * @see AdobeCallbackWithError
	 */
	void getVisitorProfile(final AdobeCallback<Map<String, String>> adobeCallback) {
		identityRequest(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE, adobeCallback);
	}

	/**
	 * Initiates an Audience Manager Identity Request event.
	 * Currently used to get dpid, dpuuid, and user profiles from aam module
	 *
	 * @param keyName  		(required) key in which this method will search the resulting event's
	 *                      eventdata for to provide in the callback
	 * @param callback 		(required) {@link AdobeCallback} method which will be called with the appropriate value depending on the keyName param
	 * @param callback      (required) {@link AdobeCallbackWithError} method which is called with the appropriate value depending on the keyName param
	 *                      or an error if there is an error with the underlying call or if it times out.
	 *
	 * @see AudienceConstants.EventDataKeys.Audience
	 */
	void identityRequest(final String keyName, final AdobeCallback<Map<String, String>> callback) {
		// both parameters are required
		if (StringUtils.isNullOrEmpty(keyName) || callback == null) {
			Log.debug(LOG_TAG, "identityRequest - Failed to send Identity request. Key name is empty or Callback is null");
			return;
		}

		final Event event = new Event.Builder("AudienceRequestIdentity",
											  EventType.AUDIENCEMANAGER, EventSource.REQUEST_IDENTITY).setPairID("").build();

		final AdobeCallbackWithError adobeCallbackWithError = callback instanceof AdobeCallbackWithError ?
				(AdobeCallbackWithError)callback : null;

		eventHub.registerOneTimeListener(event.getResponsePairID(), new Module.OneTimeListenerBlock() {
			@Override
			@SuppressWarnings("unchecked")
			public void call(final Event e) {
				final EventData eventData = e.getData();

				if (keyName.equals(AudienceConstants.EventDataKeys.Audience.AUDIENCE_IDS)) {
					final String dpid = eventData.optString("dpid", null);
					final  String dpuuid = eventData.optString("dpuuid", null);
					final Map<String, String> value = new HashMap<String, String>();
					value.put(AudienceConstants.EventDataKeys.Audience.DPID, dpid);
					value.put(AudienceConstants.EventDataKeys.Audience.DPUUID, dpuuid);
					callback.call(value);

				} else if (keyName.equals(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE)) {
					final Map<String, String> value = eventData.optStringMap(keyName, new HashMap<String, String>());
					callback.call(value);
				} else {
					Log.debug(LOG_TAG, "identityRequest - Failed to register REQUEST_IDENTITY listener");
					callback.call(null);
				}


			}
		}, adobeCallbackWithError);

		eventHub.dispatch(event);
		Log.debug(LOG_TAG, "identityRequest - Identity request was sent: %s", event);
	}

	/**
	 * Initiate an Audience Manager Identity Request event.
	 * Used to set values for dpid and dpuuid
	 *
	 * @param dpid   data provider id
	 * @param dpuuid uuid unique per data provider
	 */
	void setDataProviderIds(final String dpid, final String dpuuid) {
		final EventData data = new EventData();
		data.putString(AudienceConstants.EventDataKeys.Audience.DPID, StringUtils.isNullOrEmpty(dpid) ? "" : dpid);
		data.putString(AudienceConstants.EventDataKeys.Audience.DPUUID, StringUtils.isNullOrEmpty(dpuuid) ? "" : dpuuid);
		final Event event = new Event.Builder("AudienceSetDataProviderIds",
											  EventType.AUDIENCEMANAGER, EventSource.REQUEST_IDENTITY)
		.setData(data)
		.build();
		eventHub.dispatch(event);
		Log.debug(LOG_TAG, "setDataProviderIds - Date Provider IDs were set");
	}

	/**
	 * This method should be called from the Platform to initiate an Audience Manager Request Reset event
	 */
	void reset() {

	}

}
