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

import com.adobe.marketing.mobile.DatabaseService.QueryResult;
import com.adobe.marketing.mobile.DatabaseService.Database.ColumnDataType;
import com.adobe.marketing.mobile.DatabaseService.Database.ColumnConstraint;


/**
 * AudienceHitSchema class takes on the following role:
 * <ol>
 *     <li>Provides the format of the database table to be used by the {@link AudienceHitsDatabase} class.</li>
 *     <li>Provides methods for interfacing between a {@link DatabaseService.QueryResult} and an {@link AudienceHit}.</li>
 * </ol>
 */
class AudienceHitSchema extends AbstractHitSchema<AudienceHit> {

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
	AudienceHitSchema() {
		this.columnConstraints = new ArrayList<List<ColumnConstraint>>();
		List<ColumnConstraint> idColumnConstraints = new ArrayList<DatabaseService.Database.ColumnConstraint>();
		idColumnConstraints.add(ColumnConstraint.PRIMARY_KEY);
		idColumnConstraints.add(ColumnConstraint.AUTOINCREMENT);
		columnConstraints.add(idColumnConstraints);                  // id
		columnConstraints.add(new ArrayList<ColumnConstraint>());    // url
		columnConstraints.add(new ArrayList<ColumnConstraint>());    // timeout
		columnConstraints.add(new ArrayList<ColumnConstraint>());    // timestamp
		columnConstraints.add(new ArrayList<ColumnConstraint>());    // pair id
		columnConstraints.add(new ArrayList<ColumnConstraint>());    // event number

		this.columnNames = new String[] {
			COL_REQUESTS_ID,
			COL_REQUESTS_URL,
			COL_REQUESTS_TIMEOUT,
			COL_REQUESTS_TIMESTAMP,
			COL_REQUESTS_PAIR_ID,
			COL_REQUESTS_EVENT_NUMBER
		};

		this.columnDataTypes = new ColumnDataType[] {
			ColumnDataType.INTEGER,    // id
			ColumnDataType.TEXT,       // url
			ColumnDataType.INTEGER,    // timeout
			ColumnDataType.INTEGER,    // timestamp
			ColumnDataType.TEXT,       // pair id
			ColumnDataType.INTEGER     // event number
		};
	}

	/**
	 * Accepts a {@code QueryResult} object provided by the {@code DatabaseService} and
	 * converts it to an {@code AudienceHit} instance.
	 * <p>
	 * If {@link DatabaseService} provided {@code queryResult} is null or an exception occurs while processing the {@code queryResult},
	 * null will be returned.
	 *
	 * @param queryResult the query result provided by the {@code DatabaseService}
	 * @return {@link AudienceHit} representation of the provided {@code queryResult}
	 */
	@Override
	AudienceHit generateHit(final QueryResult queryResult) {
		if (queryResult == null) {
			Log.debug(LOG_TAG, "Unable to generate AudienceHit, query result was null");
			return null;
		}

		try {
			AudienceHit audienceHit = new AudienceHit();
			audienceHit.identifier = queryResult.getString(COL_INDEX_REQUESTS_ID);
			audienceHit.url = queryResult.getString(COL_INDEX_REQUESTS_URL);
			audienceHit.timestamp = queryResult.getInt(COL_INDEX_REQUESTS_TIMESTAMP);
			audienceHit.timeout = queryResult.getInt(COL_INDEX_REQUESTS_TIMEOUT);
			audienceHit.pairId = queryResult.getString(COL_INDEX_REQUESTS_PAIR_ID);
			audienceHit.eventNumber = queryResult.getInt(COL_INDEX_REQUESTS_EVENT_NUMBER);
			return audienceHit;
		} catch (Exception e) {
			Log.error(LOG_TAG, "Unable to read from database. Query failed with error %s", e);
			return null;
		} finally {
			queryResult.close();
		}
	}

	/**
	 * Accepts an {@code AudienceHit} instance and converts it to a {@code Map<String, Object>}.
	 * <p>
	 * The main usage for this method is to provide the parameters needed for the {@link DatabaseService} to
	 * insert a new record into the table.
	 * <p>
	 * This method intentionally leaves out the {@link #COL_REQUESTS_ID}, because that column is
	 * {@link ColumnConstraint#AUTOINCREMENT} in the database.
	 *
	 * @param hit the {@link AudienceHit} instance
	 * @return {@code Map<String, Object>} representation of the provided {@code hit}
	 */
	@Override
	Map<String, Object> generateDataMap(final AudienceHit hit) {
		if (hit == null) {
			return new HashMap<String, Object>();
		}

		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put(COL_REQUESTS_URL, hit.url);
		dataMap.put(COL_REQUESTS_TIMESTAMP, hit.timestamp);
		dataMap.put(COL_REQUESTS_TIMEOUT, hit.timeout);
		dataMap.put(COL_REQUESTS_PAIR_ID, hit.pairId);
		dataMap.put(COL_REQUESTS_EVENT_NUMBER, hit.eventNumber);
		return dataMap;
	}

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