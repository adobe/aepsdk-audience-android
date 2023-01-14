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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * AudienceHitSchema class takes on the following role:
 * <ol>
 *     <li>Provides the format of the database table to be used by the {@link AudienceHitProcessor} class.</li>
 *     <li>Provides methods for interfacing between a {@code DatabaseService.QueryResult} and an {@link AudienceDataEntity}.</li>
 * </ol>
 */
class AudienceHitSchema {

	private static final String LOG_TAG = "AudienceHitSchema";

	private static final int COL_INDEX_REQUESTS_ID = 0;
	private static final int COL_INDEX_REQUESTS_URL = 1;
	private static final int COL_INDEX_REQUESTS_TIMEOUT = 2;
	private static final int COL_INDEX_REQUESTS_TIMESTAMP = 3;
	private static final int COL_INDEX_REQUESTS_PAIR_ID = 4;
	private static final int COL_INDEX_REQUESTS_EVENT_NUMBER = 5;


	private static final String COL_REQUESTS_ID = "ID";
	private static final String COL_REQUESTS_URL = "URL";
	private static final String COL_REQUESTS_TIMEOUT = "TIMEOUT";
	private static final String COL_REQUESTS_TIMESTAMP = "TIMESTAMP";
	private static final String COL_REQUESTS_PAIR_ID = "PAIR_ID";
	private static final String COL_REQUESTS_EVENT_NUMBER = "EVENT_NUMBER";

	/**
	 * Constructor.
	 */
	AudienceHitSchema() {}

	/**
	 * Generates a {@code Map<String, Object>} which is used in a database query to reset {@link #COL_REQUESTS_PAIR_ID} and
	 * {@link #COL_REQUESTS_EVENT_NUMBER} for all existing records.
	 * <p>
	 * This method exists because a pair id and event number cannot persist between sessions. If there are any
	 * existing records in this Audience table on a new launch, these values must be reset.
	 *
	 * @return {@code Map<String, Object>} containing values needed for a database update query to reset event number
	 * and pair id
	 */
	Map<String, Object> generateUpdateValuesForResetEventNumberAndPairId() {
		final Map<String, Object> updateValues = new HashMap<String, Object>();
		updateValues.put(COL_REQUESTS_PAIR_ID, "");
		updateValues.put(COL_REQUESTS_EVENT_NUMBER, -1);
		return updateValues;
	}

}