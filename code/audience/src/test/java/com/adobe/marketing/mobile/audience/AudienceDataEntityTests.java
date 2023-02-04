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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.DataEntity;
import java.util.HashMap;
import org.junit.Test;

public class AudienceDataEntityTests {

	private static final Event TEST_EVENT = new Event.Builder("test", "all", "things").build();
	private static final String TEST_URL = "test.server.com";

	@Test
	public void testConstructor_allParams() {
		AudienceDataEntity entity = new AudienceDataEntity(TEST_EVENT, TEST_URL, 5);
		assertEquals(TEST_EVENT, entity.getEvent());
		assertEquals(TEST_URL, entity.getUrl());
		assertEquals(5, entity.getTimeoutSec());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_withNullEvent_throwsIllegalArgumentEx() {
		new AudienceDataEntity(null, TEST_URL, 5);
	}

	@Test
	public void testConstructor_whenNullUrl() {
		AudienceDataEntity entity = new AudienceDataEntity(TEST_EVENT, null, 5);
		assertNotNull(entity.getEvent());
		assertNull(entity.getUrl());
		assertEquals(5, entity.getTimeoutSec());
	}

	@Test
	public void testToFromDataEntity() {
		Event event = new Event.Builder("name", "type", "source")
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("key", "value");
					}
				}
			)
			.build();

		AudienceDataEntity entity = new AudienceDataEntity(event, TEST_URL, 5);
		DataEntity serializedEntity = entity.toDataEntity();
		assertNotNull(serializedEntity);

		AudienceDataEntity deserializedEntity = AudienceDataEntity.fromDataEntity(serializedEntity);
		assertNotNull(deserializedEntity);

		assertEquals(TEST_URL, deserializedEntity.getUrl());
		assertEquals(5, deserializedEntity.getTimeoutSec());

		// Assert Event
		Event deserializedEvent = deserializedEntity.getEvent();
		assertNotNull(deserializedEvent);
		assertEquals(event.getName(), deserializedEvent.getName());
		assertEquals(event.getType(), deserializedEvent.getType());
		assertEquals(event.getSource(), deserializedEvent.getSource());
		assertEquals(event.getUniqueIdentifier(), deserializedEvent.getUniqueIdentifier());
		assertEquals(event.getTimestamp(), deserializedEvent.getTimestamp());
		assertEquals(event.getEventData(), deserializedEvent.getEventData());
	}

	@Test
	public void testToFromDataEntity_withNullUrl() {
		AudienceDataEntity entity = new AudienceDataEntity(TEST_EVENT, null, 5);
		DataEntity serializedEntity = entity.toDataEntity();
		assertNotNull(serializedEntity);

		AudienceDataEntity deserializedEntity = AudienceDataEntity.fromDataEntity(serializedEntity);
		assertNotNull(deserializedEntity);

		assertNull(deserializedEntity.getUrl());
		assertEquals(5, deserializedEntity.getTimeoutSec());

		// Assert Event
		Event deserializedEvent = deserializedEntity.getEvent();
		assertNotNull(deserializedEvent);
		assertEquals(TEST_EVENT.getName(), deserializedEvent.getName());
		assertEquals(TEST_EVENT.getType(), deserializedEvent.getType());
		assertEquals(TEST_EVENT.getSource(), deserializedEvent.getSource());
		assertEquals(TEST_EVENT.getUniqueIdentifier(), deserializedEvent.getUniqueIdentifier());
		assertEquals(TEST_EVENT.getTimestamp(), deserializedEvent.getTimestamp());
		assertEquals(TEST_EVENT.getEventData(), deserializedEvent.getEventData());
	}

	@Test
	public void testFromDataEntity_whenDataEntityWithNullData_returnsNull() {
		assertNull(AudienceDataEntity.fromDataEntity(new DataEntity(null)));
	}

	@Test
	public void testFromDataEntity_whenDataEntityWithEmptyData_returnsNull() {
		assertNull(AudienceDataEntity.fromDataEntity(new DataEntity("")));
	}

	@Test
	public void testFromDataEntity_whenInvalidDataEntity_returnsNull() {
		assertNull(AudienceDataEntity.fromDataEntity(new DataEntity("abc")));
	}
}
