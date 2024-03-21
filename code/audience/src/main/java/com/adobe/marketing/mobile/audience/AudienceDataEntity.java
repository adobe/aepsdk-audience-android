/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.audience;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventCoder;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.Log;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

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
	AudienceDataEntity(@NonNull final Event event, final String url, final int timeoutSec)
		throws IllegalArgumentException {
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
	@Nullable static AudienceDataEntity fromDataEntity(@NotNull final DataEntity dataEntity) {
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
