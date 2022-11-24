/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.TestHelper.setNetworkEnabled
import net.waterfox.android.helpers.TestHelper.verifyUrl
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar

/**
 * Tests to verify some main UI flows with Network connection off
 *
 */

class NoNetworkAccessStartupTests {

    @get:Rule
    val activityTestRule = HomeActivityTestRule(launchActivity = false)

    private val featureSettingsHelper = FeatureSettingsHelper()

    @Before
    fun setUp() {
        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        // Restoring network connection
        setNetworkEnabled(true)
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Test
    // Test running on release builds in CI:
    // caution when making changes to it, so they don't block the builds
    // Based on STR from https://github.com/mozilla-mobile/fenix/issues/16886
    fun noNetworkConnectionStartupTest() {
        setNetworkEnabled(false)

        activityTestRule.launchActivity(null)

        homeScreen {
        }.dismissOnboarding()
        homeScreen {
            verifyHomeScreen()
        }
    }

    @Test
    // Based on STR from https://github.com/mozilla-mobile/fenix/issues/16886
    fun networkInterruptedFromBrowserToHomeTest() {
        val url = "example.com"
        val settings = InstrumentationRegistry.getInstrumentation().targetContext.settings()
        settings.shouldShowJumpBackInCFR = false

        activityTestRule.launchActivity(null)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {}

        setNetworkEnabled(false)

        browserScreen {
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    @Test
    fun testPageReloadAfterNetworkInterrupted() {
        val url = "example.com"

        activityTestRule.launchActivity(null)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {}

        setNetworkEnabled(false)

        browserScreen {
        }.openThreeDotMenu {
        }.refreshPage { }
    }

    @Ignore("Failing with frequent ANR: https://bugzilla.mozilla.org/show_bug.cgi?id=1764605")
    @Test
    fun testSignInPageWithNoNetworkConnection() {
        setNetworkEnabled(false)

        activityTestRule.launchActivity(null)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openTurnOnSyncMenu {
            tapOnUseEmailToSignIn()
            verifyUrl(
                "waterfox.net",
                "$packageName:id/mozac_browser_toolbar_url_view",
                R.id.mozac_browser_toolbar_url_view
            )
        }
    }
}
