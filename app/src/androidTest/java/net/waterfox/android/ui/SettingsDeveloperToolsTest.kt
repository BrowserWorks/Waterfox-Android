/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.ui.robots.homeScreen

/**
 *  Tests for verifying the main three dot menu options
 *
 */

class SettingsDeveloperToolsTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

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
    // Walks through settings developer tools menu and sub-menus to ensure all items are present
    fun settingsDeveloperToolsItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyRemoteDebug()
        }
    }

    // DEVELOPER TOOLS
    @Ignore("This is a stub test, ignore for now")
    @Test
    fun turnOnRemoteDebuggingViaUsb() {
        // Open terminal
        // Verify USB debugging is off
        // Open 3dot (main) menu
        // Select settings
        // Toggle Remote debugging via USB to 'on'
        // Open terminal
        // Verify USB debugging is on
    }

    // ABOUT
    @Ignore("This is a stub test, ignore for now")
    @Test
    fun verifyHelpRedirect() {
        // Open 3dot (main) menu
        // Select settings
        // Click on "Help"
        // Verify redirect to: https://support.mozilla.org/
    }

    @Ignore("This is a stub test, ignore for now")
    @Test
    fun verifyRateOnGooglePlayRedirect() {
        // Open 3dot (main) menu
        // Select settings
        // Click on "Rate on Google Play"
        // Verify Android "Open with Google Play Store" sub menu
    }

    @Ignore("This is a stub test, ignore for now")
    @Test
    fun verifyAboutWaterfoxPreview() {
        // Open 3dot (main) menu
        // Select settings
        // Click on "Verify About Waterfox Preview"
        // Verify about page contains....
        // Build #
        // Version #
        // "Waterfox Preview is produced by WaterfoxCo"
        // Day, Date, timestamp
        // "Open source libraries we use"
    }
}
