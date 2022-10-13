/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.core.net.toUri
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.R
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.ui.robots.navigationToolbar

/**
 * Tests that verify errors encountered while browsing websites: unsafe pages, connection errors, etc
 */
class BrowsingErrorPagesTest {
    private val malwareWarning = getStringResource(R.string.mozac_browser_errorpages_safe_browsing_malware_uri_title)
    private val phishingWarning = getStringResource(R.string.mozac_browser_errorpages_safe_phishing_uri_title)
    private val unwantedSoftwareWarning =
        getStringResource(R.string.mozac_browser_errorpages_safe_browsing_unwanted_uri_title)
    private val harmfulSiteWarning = getStringResource(R.string.mozac_browser_errorpages_safe_harmful_uri_title)
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get: Rule
    val mActivityTestRule = HomeActivityTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        // disabling the jump-back-in pop-up that interferes with the tests.
        featureSettingsHelper.setJumpBackCFREnabled(false)
    }

    @After
    fun tearDown() {
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun blockMalwarePageTest() {
        val malwareURl = "http://itisatrap.org/waterfox/its-an-attack.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(malwareURl.toUri()) {
            verifyPageContent(malwareWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockPhishingPageTest() {
        val phishingURl = "http://itisatrap.org/waterfox/its-a-trap.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(phishingURl.toUri()) {
            verifyPageContent(phishingWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockUnwantedSoftwarePageTest() {
        val unwantedURl = "http://itisatrap.org/waterfox/unwanted.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(unwantedURl.toUri()) {
            verifyPageContent(unwantedSoftwareWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockHarmfulPageTest() {
        val harmfulURl = "https://itisatrap.org/waterfox/harmful.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(harmfulURl.toUri()) {
            verifyPageContent(harmfulSiteWarning)
        }
    }
}
