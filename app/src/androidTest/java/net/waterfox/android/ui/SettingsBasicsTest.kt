/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import android.content.res.Configuration
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.WaterfoxApplication
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.TestAssetHelper.getLoremIpsumAsset
import net.waterfox.android.ui.robots.checkTextSizeOnWebsite
import net.waterfox.android.ui.robots.homeScreen

/**
 *  Tests for verifying the General section of the Settings menu
 *
 */
class SettingsBasicsTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    private lateinit var mockWebServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        featureSettingsHelper.setJumpBackCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()

        // resetting modified features enabled setting to default
        featureSettingsHelper.resetAllFeatureFlags()
    }

    private fun getUiTheme(): Boolean {
        val mode =
            activityIntentTestRule.activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)

        return when (mode) {
            Configuration.UI_MODE_NIGHT_YES -> true // dark theme is set
            Configuration.UI_MODE_NIGHT_NO -> false // dark theme is not set, using light theme
            else -> false // default option is light theme
        }
    }

    @Test
    fun settingsGeneralItemsTests() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsToolbar()
            verifyGeneralHeading()
            verifySearchButton()
            verifyTabsButton()
            verifyHomepageButton()
            verifyCustomizeButton()
            verifyLoginsAndPasswordsButton()
            verifyAutofillButton()
            verifyAccessibilityButton()
            verifyLanguageButton()
            verifySetAsDefaultBrowserButton()
        }
    }

    @Test
    fun changeThemeSetting() {
        // Goes through the settings and changes the default search engine, then verifies it changes.
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            verifyThemes()
            selectDarkMode()
            verifyDarkThemeApplied(getUiTheme())
            selectLightMode()
            verifyLightThemeApplied(getUiTheme())
        }
    }

    @Test
    fun changeAccessibiltySettings() {
        // Goes through the settings and changes the default text on a webpage, then verifies if the text has changed.
        val waterfoxApp = activityIntentTestRule.activity.applicationContext as WaterfoxApplication
        val webpage = getLoremIpsumAsset(mockWebServer).url

        // This value will represent the text size percentage the webpage will scale to. The default value is 100%.
        val textSizePercentage = 180

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAccessibilitySubMenu {
            clickFontSizingSwitch()
            verifyEnabledMenuItems()
            changeTextSizeSlider(textSizePercentage)
            verifyTextSizePercentage(textSizePercentage)
        }.goBack {
        }.goBack {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(webpage) {
            checkTextSizeOnWebsite(textSizePercentage, waterfoxApp.components)
        }.openThreeDotMenu {
        }.openSettings {
        }.openAccessibilitySubMenu {
            clickFontSizingSwitch()
            verifyMenuItemsAreDisabled()
        }
    }
}
