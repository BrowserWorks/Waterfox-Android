/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.R
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar

/**
 * Tests Top Sites functionality
 *
 * - Verifies 'Add to Waterfox Home' UI functionality
 * - Verifies 'Top Sites' context menu UI functionality
 * - Verifies 'Top Site' usage UI functionality
 * - Verifies existence of default top sites available on the home-screen
 */

class TopSitesTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule(skipOnboarding = true)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        featureSettingsHelper.setJumpBackCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun verifyAddToWaterfoxHome() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }
    }

    @Test
    fun verifyOpenTopSiteNormalTab() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openTopSiteTabWithTitle(title = defaultWebPageTitle) {
            verifyUrl(defaultWebPage.url.toString().replace("http://", ""))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openContextMenuOnTopSitesWithTitle(defaultWebPageTitle) {
            verifyTopSiteContextMenuItems()
        }

        // Dismiss context menu popup
        mDevice.pressBack()
    }

    @Test
    fun verifyOpenTopSitePrivateTab() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openContextMenuOnTopSitesWithTitle(defaultWebPageTitle) {
            verifyTopSiteContextMenuItems()
        }.openTopSiteInPrivateTab {
            verifyCurrentPrivateSession(activityIntentTestRule.activity.applicationContext)
        }
    }

    @Test
    fun verifyRenameTopSite() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"
        val defaultWebPageTitleNew = "Test_Page_2"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openContextMenuOnTopSitesWithTitle(defaultWebPageTitle) {
            verifyTopSiteContextMenuItems()
        }.renameTopSite(defaultWebPageTitleNew) {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitleNew)
        }
    }

    @Test
    fun verifyRemoveTopSite() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openContextMenuOnTopSitesWithTitle(defaultWebPageTitle) {
            verifyTopSiteContextMenuItems()
        }.removeTopSite {
            verifyNotExistingTopSitesList(defaultWebPageTitle)
        }
    }

    @Test
    fun verifyRemoveTopSiteFromMainMenu() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val defaultWebPageTitle = "Test_Page_1"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            expandMenu()
            verifyAddToTopSitesButton()
        }.addToWaterfoxHome {
            verifySnackBarText(getStringResource(R.string.snackbar_added_to_shortcuts))
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPageTitle)
        }.openTopSiteTabWithTitle(defaultWebPageTitle) {
        }.openThreeDotMenu {
            verifyRemoveFromShortcutsButton()
        }.clickRemoveFromShortcuts {
        }.goToHomescreen {
            verifyNotExistingTopSitesList(defaultWebPageTitle)
        }
    }

    @Test
    fun verifyDefaultTopSitesLocale_EN() {
        // en-US defaults
        val defaultTopSites = arrayOf(
            "Top Articles",
            "Wikipedia",
            "Google"
        )

        homeScreen { }.dismissOnboarding()

        homeScreen {
            verifyExistingTopSitesList()
            defaultTopSites.forEach { item ->
                verifyExistingTopSitesTabs(item)
            }
        }
    }

    @SmokeTest
    @Test
    fun addAndRemoveMostViewedTopSiteTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        for (i in 0..1) {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                mDevice.waitForIdle()
                waitForPageToLoad()
            }
        }

        browserScreen {
        }.goToHomescreen {
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs(defaultWebPage.title)
        }.openContextMenuOnTopSitesWithTitle(defaultWebPage.title) {
        }.deleteTopSiteFromHistory {
        }.openThreeDotMenu {
        }.openHistory {
            verifyEmptyHistoryView()
        }
    }
}
