/*******************************************************************************
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
 ******************************************************************************/

package com.adobe.marketing.mobile.audience;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class AudienceStateTests extends BaseTest {

	// event data keys
	private static final String EVENT_DATA_VISITOR_PROFILE = "aamprofile";
	private static final String EVENT_DATA_DPID            = "dpid";
	private static final String EVENT_DATA_DPUUID          = "dpuuid";
	private static final String EVENT_DATA_UUID			   = "uuid";


	private LocalStorageService localStorageService;
	private LocalStorageService.DataStore audienceDataStore;
	private AudienceState audienceState;

	private static String DPID = "dpid";
	private static String DPUUID = "dpuuid";
	private static String UUID = "uuid";
	private static Map<String, String> VISITOR_PROFILE;
	static {
		VISITOR_PROFILE = new HashMap<String, String>();
		VISITOR_PROFILE.put("trait", "value");
	}


	@Before
	public void setup() {
		super.beforeEach();

		localStorageService = platformServices.getLocalStorageService();
		audienceDataStore = localStorageService.getDataStore(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_DATA_STORE);
		audienceState = new AudienceState(localStorageService);
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
	}


	// ============================================================
	// GetDpid()
	// ============================================================

	@Test
	public void getDpidInMemory() {
		audienceState.setDpid(DPID);
		String dpid = audienceState.getDpid();
		assertEquals(DPID, dpid);
	}

	@Test
	public void getDpidEmptyInMemory() {
		audienceState.setDpid("");
		String dpid = audienceState.getDpid();
		assertEquals("", dpid);
	}

	@Test
	public void getDpidNullInMemory() {
		audienceState.setDpid(null);
		String dpid = audienceState.getDpid();
		assertEquals(null, dpid);
	}

	// ============================================================
	// GetDpuuid()
	// ============================================================

	@Test
	public void getDpuuidInMemory() {
		audienceState.setDpuuid(DPUUID);
		String dpuuid = audienceState.getDpuuid();
		assertEquals(DPUUID, dpuuid);
	}

	@Test
	public void getDpuuidEmptyInMemory() {
		audienceState.setDpuuid("");
		String dpuuid = audienceState.getDpuuid();
		assertEquals("", dpuuid);
	}

	@Test
	public void getDpuuidNullInMemory() {
		audienceState.setDpuuid(null);
		String dpuuid = audienceState.getDpuuid();
		assertEquals(null, dpuuid);
	}

	// ============================================================
	// GetUuid()
	// ============================================================

	@Test
	public void getUuidInMemory() {
		audienceState.setUuid(UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);
	}

	@Test
	public void getUuidInPersistence() {
		audienceDataStore.setString(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY, UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);
	}

	@Test
	public void getUuidEmptyInMemory() {
		audienceState.setUuid("");
		String uuid = audienceState.getUuid();
		assertEquals("", uuid);
	}

	@Test
	public void getUuidNullInMemory() {
		audienceState.setUuid(null);
		String uuid = audienceState.getUuid();
		assertEquals(null, uuid);
	}

	// ============================================================
	// GetVisitorProfile()
	// ============================================================

	@Test
	public void getVisitorProfileInMemory() {
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);
	}

	@Test
	public void getVisitorProfileInPersistence() {
		audienceDataStore.setMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);
	}

	@Test
	public void getVisitorProfileEmptyInMemory() {
		audienceState.setVisitorProfile(Collections.<String, String>emptyMap());
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertTrue(profile.isEmpty());
	}

	@Test
	public void getVisitorProfileNullInMemory() {
		audienceState.setVisitorProfile(null);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(null, profile);
	}

	// ============================================================
	// SetDpid()
	// ============================================================

	@Test
	public void setDpidPrivacyOptIn() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setDpid(DPID);
		String dpid = audienceState.getDpid();
		assertEquals(DPID, dpid);
	}

	@Test
	public void setDpidPrivacyOptOut() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setDpid(DPID);
		String dpid = audienceState.getDpid();
		assertEquals(null, dpid);
	}

	@Test
	public void setDpidPrivacyUnknown() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setDpid(DPID);
		String dpid = audienceState.getDpid();
		assertEquals(DPID, dpid);
	}

	// ============================================================
	// SetDpuuid()
	// ============================================================

	@Test
	public void setDpuuidPrivacyOptIn() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setDpuuid(DPUUID);
		String dpuuid = audienceState.getDpuuid();
		assertEquals(DPUUID, dpuuid);
	}

	@Test
	public void setDpuuidPrivacyOptOut() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setDpuuid(DPUUID);
		String dpuuid = audienceState.getDpuuid();
		assertEquals(null, dpuuid);
	}

	@Test
	public void setDpuuidPrivacyUnknown() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setDpuuid(DPUUID);
		String dpuuid = audienceState.getDpuuid();
		assertEquals(DPUUID, dpuuid);
	}

	// ============================================================
	// SetUuid()
	// ============================================================

	@Test
	public void setUuidPrivacyOptIn() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setUuid(UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);

		uuid = audienceDataStore.getString(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY, "default");
		assertEquals(UUID, uuid);
	}

	@Test
	public void setUuidPrivacyOptOut() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setUuid(UUID);
		String uuid = audienceState.getDpuuid();
		assertEquals(null, uuid);

		assertFalse(audienceDataStore.contains(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
	}

	@Test
	public void setUuidPrivacyUnknown() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setUuid(UUID);
		String uuid = audienceState.getUuid();
		assertEquals(UUID, uuid);

		uuid = audienceDataStore.getString(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY, "default");
		assertEquals(UUID, uuid);
	}

	// ============================================================
	// SetVisitorProfile()
	// ============================================================

	@Test
	public void setVisitorProfilePrivacyOptIn() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);

		profile = audienceDataStore.getMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY);
		assertEquals(VISITOR_PROFILE, profile);
	}

	@Test
	public void setVisitorProfilePrivacyOptOut() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertNull(profile);

		assertFalse(audienceDataStore.contains(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	@Test
	public void setVisitorProfilePrivacyUnknown() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
		audienceState.setVisitorProfile(VISITOR_PROFILE);
		Map<String, String> profile = audienceState.getVisitorProfile();
		assertEquals(VISITOR_PROFILE, profile);

		profile = audienceDataStore.getMap(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY);
		assertEquals(VISITOR_PROFILE, profile);
	}

	// ============================================================
	// ClearIdentifiers()
	// ============================================================

	@Test
	public void clearIdentifiers() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setDpid(DPID);
		audienceState.setDpuuid(DPUUID);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		audienceState.clearIdentifiers();

		assertNull(audienceState.getDpid());
		assertNull(audienceState.getDpuuid());
		assertNull(audienceState.getUuid());
		assertNull(audienceState.getVisitorProfile());

		assertFalse(audienceDataStore.contains(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY));
		assertFalse(audienceDataStore.contains(AudienceTestConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY));
	}

	// ============================================================
	// GetStateData()
	// ============================================================

	@Test
	public void getStateData() throws Exception {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_IN);
		audienceState.setDpid(DPID);
		audienceState.setDpuuid(DPUUID);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		EventData data = audienceState.getStateData();

		assertEquals(DPID, data.optString(EVENT_DATA_DPID, "default"));
		assertEquals(DPUUID, data.optString(EVENT_DATA_DPUUID, "default"));
		assertEquals(UUID, data.optString(EVENT_DATA_UUID, "default"));
		assertEquals(VISITOR_PROFILE, data.getStringMap(EVENT_DATA_VISITOR_PROFILE));
	}

	@Test
	public void getStateDataEmptyOnOptOut() {
		audienceState.setMobilePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		audienceState.setDpid(DPID);
		audienceState.setDpuuid(DPUUID);
		audienceState.setUuid(UUID);
		audienceState.setVisitorProfile(VISITOR_PROFILE);

		EventData data = audienceState.getStateData();

		assertFalse(data.containsKey(EVENT_DATA_DPID));
		assertFalse(data.containsKey(EVENT_DATA_DPUUID));
		assertFalse(data.containsKey(EVENT_DATA_UUID));
		assertFalse(data.containsKey(EVENT_DATA_VISITOR_PROFILE));
	}

}
