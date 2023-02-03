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

import com.adobe.marketing.mobile.services.HttpConnecting;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MockConnection implements HttpConnecting {

	private final int responseCode;
	private final String payload;

	MockConnection(final int responseCode, final String payload) {
		this.responseCode = responseCode;
		this.payload = payload;
	}

	@Override
	public InputStream getInputStream() {
		if (payload != null) {
			return new ByteArrayInputStream(payload.getBytes());
		}
		return null;
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}

	@Override
	public int getResponseCode() {
		return responseCode;
	}

	@Override
	public String getResponseMessage() {
		return null;
	}

	@Override
	public String getResponsePropertyValue(String s) {
		return null;
	}

	@Override
	public void close() {}
}
