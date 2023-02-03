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

import static com.adobe.marketing.mobile.audience.AudienceTestConstants.LOG_TAG;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobileCoreHelper;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestHelper {

	private static final String LOG_SOURCE = "TestHelper";
	private static final int WAIT_EVENT_TIMEOUT_MS = 2000;
	private static final long WAIT_SHARED_STATE_MS = 5000;
	private static final long REGISTRATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);

	private static final ArrayList<String> knownDatabases = new ArrayList<String>() {
		{
			add(AudienceTestConstants.Database.AUDIENCE);
			add(AudienceTestConstants.Database.IDENTITY);
		}
	};

	static Application defaultApplication;

	/**
	 * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
	 * tearsdown the MobileCore after test execution.
	 *
	 * To use, add the following to your test class:
	 * <pre>
	 * 	@Rule
	 * 	public TestHelper.SetupCoreRule coreRule = new TestHelper.SetupCoreRule();
	 * </pre>
	 */
	public static class SetupCoreRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (defaultApplication == null) {
						Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
						defaultApplication = Instrumentation.newApplication(CustomApplication.class, context);
					}

					MobileCore.setLogLevel(LoggingMode.VERBOSE);
					MobileCore.setApplication(defaultApplication);

					try {
						base.evaluate();
					} catch (Throwable e) {
						Log.debug(LOG_TAG, "SetupCoreRule", "Wait after test failure.");
						throw e; // rethrow test failure
					} finally {
						// After test execution
						Log.debug(LOG_TAG, "SetupCoreRule", "Finished '" + description.getMethodName() + "'");
						MobileCoreHelper.resetSDK();
						TestPersistenceHelper.resetKnownPersistence();
					}
				}
			};
		}
	}

	/**
	 * Applies the configuration provided, registers the extensions and then starts
	 * core.
	 * @param extensions the extensions that need to be registered
	 * @param configuration the initial configuration update that needs to be applied
	 * @throws InterruptedException if the wait time for extension registration has elapsed
	 */
	public static void registerExtensions(
		final List<Class<? extends Extension>> extensions,
		@Nullable final Map<String, Object> configuration
	) throws InterruptedException {
		if (configuration != null) {
			MobileCore.updateConfiguration(configuration);
		}

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		MobileCore.registerExtensions(extensions, o -> latch.countDown());
		latch.await(REGISTRATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Synchronous call to get the shared state for the specified {@code stateOwner}.
	 * This API throws an assertion failure in case of timeout.
	 * @param stateOwner the owner extension of the shared state (typically the name of the extension)
	 * @param timeout how long should this method wait for the requested shared state, in milliseconds
	 * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
	 * @throws InterruptedException
	 */
	public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout)
		throws InterruptedException {
		Event event = new Event.Builder(
			"Get Shared State Request",
			AudienceTestConstants.EventType.MONITOR,
			AudienceTestConstants.EventSource.SHARED_STATE_REQUEST
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(AudienceTestConstants.EventDataKey.STATE_OWNER, stateOwner);
					}
				}
			)
			.build();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		final Map<String, Object> sharedState = new HashMap<>();
		MobileCore.dispatchEventWithResponseCallback(
			event,
			WAIT_EVENT_TIMEOUT_MS,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(AdobeError adobeError) {
					Log.error(
						LOG_TAG,
						LOG_SOURCE,
						"Failed to get shared state for " + stateOwner + ": " + adobeError.getErrorName()
					);
				}

				@Override
				public void call(Event event) {
					if (event.getEventData() != null) {
						sharedState.putAll(event.getEventData());
					}

					latch.countDown();
				}
			}
		);
		assertTrue("Timeout waiting for shared state " + stateOwner, latch.await(timeout, TimeUnit.MILLISECONDS));
		return sharedState.isEmpty() ? null : sharedState;
	}

	/**
	 * Pause test execution for the given {@code milliseconds}
	 * @param milliseconds the time to sleep the current thread.
	 */
	public static void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Clears known databases; used for full cleanup between tests
	 */
	public static void resetKnownDatabases() {
		for (String database : knownDatabases) {
			DataQueue db = ServiceProvider.getInstance().getDataQueueService().getDataQueue(database);
			db.clear();
		}
	}

	/**
	 * Dummy Application for the test instrumentation
	 */
	public static class CustomApplication extends Application {

		public CustomApplication() {}
	}
}
