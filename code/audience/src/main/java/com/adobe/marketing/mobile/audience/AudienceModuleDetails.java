/* ***********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2020 Adobe Systems Incorporated
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
 *
 **************************************************************************/

package com.adobe.marketing.mobile.audience;

import java.util.HashMap;
import java.util.Map;

final class AudienceModuleDetails implements ModuleDetails {
	private final String FRIENDLY_NAME = "Audience";

	public String getName() {
		return FRIENDLY_NAME;
	}

	public String getVersion() {
		return Audience.extensionVersion();
	}

	public Map<String, String> getAdditionalInfo() {
		return new HashMap<>();
	}
}