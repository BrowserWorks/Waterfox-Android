/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.ui.robots.clickRateButtonGooglePlay
import net.waterfox.android.ui.robots.homeScreen

/**
 *  Tests for verifying the main three dot menu options
 *
 */

class SettingsAboutTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    // Walks through settings menu and sub-menus to ensure all items are present
    fun settingsAboutItemsTest() {
        // ABOUT
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            // ABOUT
            verifyAboutHeading()
            verifyRateOnGooglePlay()
            verifyAboutWaterfoxPreview()
        }
    }

    // ABOUT
    @Test
    fun verifyRateOnGooglePlayRedirect() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            clickRateButtonGooglePlay()
            verifyGooglePlayRedirect()
            // press back to return to the app, or accept ToS if still visible
            mDevice.pressBack()
            dismissGooglePlayToS()
        }
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/fenix/issues/25355")
    @Test
    fun verifyAboutWaterfoxPreview() {
        val settings = activityIntentTestRule.activity.settings()
        settings.shouldShowJumpBackInCFR = false
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAboutWaterfoxPreview {
            verifyAboutWaterfoxPreview()
        }
    }
}

private fun dismissGooglePlayToS() {
    if (mDevice.findObject(UiSelector().textContains("Terms of Service")).exists()) {
        mDevice.findObject(UiSelector().textContains("ACCEPT")).click()
    }
}
