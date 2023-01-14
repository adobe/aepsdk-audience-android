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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventCoder;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Class that encapsulates the data to be queued persistently for the {@link AudienceExtension}
 */
class AudienceDataEntity {
	private static final String LOG_SOURCE = "AudienceDataEntity";

	private static final String EVENT_KEY = "event";
	private static final String URL_KEY = "url";
	private static final String TIMEOUT_KEY = "timeoutSec";

	private final Event event;
	private final String url;
	private final int timeoutSec;

	/**
	 * Creates a read-only {@link AudienceDataEntity} object with the provided information.
	 *
	 * @param event an {@link Event}, should not be null
	 * @param url the URL for the Audience Manager request
	 * @param timeoutSec indicates the number of seconds a network request should wait for server response
	 * @throws IllegalArgumentException if the provided {@code event} is null
	 */
	AudienceDataEntity(@NonNull final Event event, final String url, final int timeoutSec) throws IllegalArgumentException {
		if (event == null) {
			throw new IllegalArgumentException();
		}

		this.event = event;
		this.url = url;
		this.timeoutSec = timeoutSec;
	}

	/**
	 * @return the {@link Event} cannot be null based on constructor
	 */
	Event getEvent() {
		return event;
	}

	/**
	 * @return the URL for the Audience Manager request
	 */
	String getUrl() {
		return url;
	}

	/**
	 * @return the number of seconds a network request should wait for server response
	 */
	int getTimeoutSec() {
		return timeoutSec;
	}

	/**
	 * Serializes this to a {@code DataEntity}.
	 * @return serialized {@code AudienceDataEntity} or null if it could not be serialized.
	 */
	@Nullable DataEntity toDataEntity() {
		try {
			JSONObject serializedEntity = new JSONObject();
			serializedEntity.put(EVENT_KEY, new JSONObject(EventCoder.encode(this.event)));
			serializedEntity.put(URL_KEY, this.url);
			serializedEntity.put(TIMEOUT_KEY, this.timeoutSec);

			return new DataEntity(
					event.getUniqueIdentifier(),
					new Date(event.getTimestamp()),
					serializedEntity.toString()
			);
		} catch (JSONException e) {
			Log.debug(
					AudienceConstants.LOG_TAG,
					LOG_SOURCE,
					"Failed to serialize AudienceDataEntity to DataEntity: " + e.getLocalizedMessage()
			);
		}

		return null;
	}

	/**
	 * Deserializes a {@code DataEntity} to a {@code AudienceDataEntity}.
	 * @param dataEntity {@code DataEntity} to be processed
	 * @return a deserialized {@code AudienceDataEntity} instance or null if it
	 * could not be deserialized to an {@code AudienceDataEntity}
	 */
	@Nullable
	static AudienceDataEntity fromDataEntity(@NotNull final DataEntity dataEntity) {
		String entity = dataEntity.getData();
		if (entity == null || entity.isEmpty()) {
			return null;
		}

		try {
			JSONObject serializedEntity = new JSONObject(entity);

			String url = null;
			if (serializedEntity.has(URL_KEY)) {
				url = serializedEntity.getString(URL_KEY);
			}

			int timeout = AudienceConstants.DEFAULT_AAM_TIMEOUT;
			if (serializedEntity.has(TIMEOUT_KEY)) {
				timeout = serializedEntity.getInt(TIMEOUT_KEY);
			}

			String eventString = serializedEntity.getJSONObject(EVENT_KEY).toString();
			Event event = EventCoder.decode(eventString);

			return new AudienceDataEntity(event, url, timeout);
		} catch (JSONException | IllegalArgumentException e) {
			Log.debug(
					AudienceConstants.LOG_TAG,
					LOG_SOURCE,
					"Failed to deserialize DataEntity to AudienceDataEntity: " + e.getLocalizedMessage()
			);
		}

		return null;
	}
}
