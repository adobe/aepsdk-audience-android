/*
  Copyright 2018 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AudienceStateTests {

	// event data keys
	private static final String EVENT_DATA_VISITOR_PROFILE = "aamprofile";
	private static final String EVENT_DATA_UUID = "uuid";
	private AudienceState audienceState;

	private static final String UUID = "uuid";
	private static final Map<String, String> VISITOR_PROFILE;

	static {
		VISITOR_PROFILE = new HashMap<>();
		VISITOR_PROFILE.put("trait", "value");
	}

	@Mock
	NamedCollection mockNamedCollection;

	@Before
	public void setup() {
		audienceState = new AudienceState(mockNamedCollection);
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
	}

	// ============================================================
	// GetUuid()
	// ============================================================

	@Test
	public void testGetUuid_returnsInMemoryValue() {
		audienceState.setUuid(UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);
	}

	@Test
	public void testGetUuid_whenSetValid_returnsValueFromPersistence() {
		when(mockNamedCollection.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any()))
			.thenReturn(UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);

		verify(mockNamedCollection)
			.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any());
	}

	@Test
	public void testGetUuid_whenSetToEmpty_returnsNull() {
		when(mockNamedCollection.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any()))
			.thenReturn(null);
		audienceState.setUuid("");
		String uuid = audienceState.getUuid();
		assertNull(uuid);

		verify(mockNamedCollection)
			.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any());
	}

	@Test
	public void getUuid_whenSetToNull_returnsNull() {
		when(mockNamedCollection.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any()))
			.thenReturn(null);
		audienceState.setUuid(null);
		String uuid = audienceState.getUuid();
		assertNull(uuid);

		verify(mockNamedCollection)
			.getString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any());
	}

	@Test
	public void testGetSetUuid_whenNullNamedCollection_doNotCrash() {
		audienceState = new AudienceState(null);
		try {
			audienceState.getUuid();
			audienceState.setUuid(UUID);
		} catch (Exception e) {
			fail("Unexpected exception thrown when null NamedCollection");
		}
	}

	// ============================================================
	// GetVisitorProfile()
	// ============================================================

	@Test
	public void testGetVisitorProfile_returnsInMemoryValue() {
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);
	}

	@Test
	public void testGetVisitorProfile_whenSetValid_returnsValueFromPersistence() {
		when(mockNamedCollection.getMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY))
			.thenReturn(VISITOR_PROFILE);
		when(mockNamedCollection.contains(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY))
			.thenReturn(true);

		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);

		verify(mockNamedCollection).getMap(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	@Test
	public void testGetVisitorProfile_whenSetEmpty_returnsEmpty() {
		audienceState.setVisitorProfile(Collections.emptyMap());
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertTrue(profile.isEmpty());
	}

	@Test
	public void getVisitorProfile_whenSetNull_returnsNull() {
		audienceState.setVisitorProfile(null);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertNull(profile);
	}

	@Test
	public void testGetSetVisitorProfile_whenNullNamedCollection_doNotCrash() {
		audienceState = new AudienceState(null);
		try {
			audienceState.getVisitorProfile();
			audienceState.setVisitorProfile(VISITOR_PROFILE);
		} catch (Exception e) {
			fail("Unexpected exception thrown when null NamedCollection");
		}
	}

	// ============================================================
	// SetUuid()
	// ============================================================

	@Test
	public void testSetUuid_whenPrivacyOptIn_updatesPersistedValue() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		assertEquals(UUID, audienceState.getUuid());

		ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockNamedCollection)
			.setString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), uuidCaptor.capture());
		assertEquals(UUID, uuidCaptor.getValue());
	}

	@Test
	public void testSetUuid_whenPrivacyOptOut_ignoresCommand() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setUuid(UUID);
		assertNull(audienceState.getUuid());

		verify(mockNamedCollection, never())
			.setString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), any());
	}

	@Test
	public void testSetUuid_whenPrivacyUnknown_updatesPersistedValue() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setUuid(UUID);
		assertEquals(UUID, audienceState.getUuid());

		ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockNamedCollection)
			.setString(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY), uuidCaptor.capture());
		assertEquals(UUID, uuidCaptor.getValue());
	}

	// ============================================================
	// SetVisitorProfile()
	// ============================================================

	@Test
	public void testSetVisitorProfile_whenPrivacyOptIn_updatesPersistedValue() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);

		ArgumentCaptor<Map<String, String>> profileCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockNamedCollection)
			.setMap(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY), profileCaptor.capture());
		assertEquals(VISITOR_PROFILE, profileCaptor.getValue());
	}

	@Test
	public void testSetVisitorProfile_whenPrivacyOptOut_ignoresCommand() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertNull(profile);

		verify(mockNamedCollection, never())
			.setMap(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY), any());
	}

	@Test
	public void testSetVisitorProfile_whenPrivacyUnknown_updatesPersistedValue() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);

		ArgumentCaptor<Map<String, String>> profileCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockNamedCollection)
			.setMap(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY), profileCaptor.capture());
		assertEquals(VISITOR_PROFILE, profileCaptor.getValue());
	}

	// ============================================================
	// ClearIdentifiers()
	// ============================================================

	@Test
	public void testClearIdentifiers_clearsPersistedUUIDAndVisitorProfile() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		audienceState.clearIdentifiers();

		assertNull(audienceState.getUuid());
		assertNull(audienceState.getVisitorProfile());

		verify(mockNamedCollection).remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
		verify(mockNamedCollection).remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	// ============================================================
	// setMobilePrivacyStatus()
	// ============================================================

	@Test
	public void testSetMobilePrivacyStatus_whenOptIn_doesNotClearPersistedUUIDAndVisitorProfile() {
		// setup
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		// test
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);

		assertEquals(UUID, audienceState.getUuid());
		assertEquals(VISITOR_PROFILE, audienceState.getVisitorProfile());

		verify(mockNamedCollection, never())
			.remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
		verify(mockNamedCollection, never())
			.remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	@Test
	public void testSetMobilePrivacyStatus_whenOptUnknown_doesNotClearPersistedUUIDAndVisitorProfile() {
		// setup
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		// test
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);

		assertEquals(UUID, audienceState.getUuid());
		assertEquals(VISITOR_PROFILE, audienceState.getVisitorProfile());

		verify(mockNamedCollection, never())
			.remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
		verify(mockNamedCollection, never())
			.remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	@Test
	public void testSetMobilePrivacyStatus_whenOptedOut_clearsPersistedUUIDAndVisitorProfile() {
		// setup
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		// test
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);

		assertNull(audienceState.getUuid());
		assertNull(audienceState.getVisitorProfile());

		verify(mockNamedCollection).remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
		verify(mockNamedCollection).remove(eq(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	// ============================================================
	// GetStateData()
	// ============================================================

	@Test
	public void testGetStateData_returnsUUIDAndVisitorProfile() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		Map<String, Object> data = audienceState.getStateData();

		assertEquals(UUID, DataReader.optString(data, EVENT_DATA_UUID, "default"));
		assertEquals(VISITOR_PROFILE, DataReader.optStringMap(data, EVENT_DATA_VISITOR_PROFILE, null));
	}

	@Test
	public void testGetStateData_whenPrivacyOptOut_returnsEmpty() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		Map<String, Object> data = audienceState.getStateData();

		assertFalse(data.containsKey(EVENT_DATA_UUID));
		assertFalse(data.containsKey(EVENT_DATA_VISITOR_PROFILE));
	}

	// ============================================================
	// Set / GetLastResetTimestamp()
	// ============================================================
	@Test
	public void testSetLastResetTimestamp_whenValidValue_updates() {
		Event testEvent = new Event.Builder("test", "testtype", "testsource").build();
		audienceState.setLastResetTimestamp(testEvent.getTimestamp());

		assertEquals(testEvent.getTimestamp(), audienceState.getLastResetTimestampMillis());
	}

	@Test
	public void testSetLastResetTimestamp_whenNegativeValue_ignores() {
		audienceState.setLastResetTimestamp(-1);

		assertEquals(0, audienceState.getLastResetTimestampMillis());
	}

	@Test
	public void testGetLastResetTimestamp_whenBootedUp_returnZero() {
		assertEquals(0, audienceState.getLastResetTimestampMillis());
	}
}
