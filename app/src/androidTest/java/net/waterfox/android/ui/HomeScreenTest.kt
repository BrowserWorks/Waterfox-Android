/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import okhttp3.mockwebserver.MockWebServer
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.ext.waitNotNull
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar
import org.junit.*

/**
 *  Tests for verifying the presence of home screen and first-run homescreen elements
 *
 *  Note: For private browsing, navigation bar and tabs see separate test class
 *
 */

class HomeScreenTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

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

        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Test
    fun homeScreenItemsTest() {
        homeScreen { }.dismissOnboarding()

        homeScreen {
            verifyHomeScreen()
            verifyNavigationToolbar()
            verifyHomePrivateBrowsingButton()
            verifyHomeMenu()
            verifyHomeWordmark()
            verifyTabButton()
            verifyCollectionsHeader()
            verifyHomeToolbar()
            verifyHomeComponent()

            // Verify Top Sites
            verifyExistingTopSitesList()
            verifyExistingTopSitesTabs("Wikipedia")
            verifyExistingTopSitesTabs("Top Articles")
            verifyExistingTopSitesTabs("Google")
        }
    }

    @Test
    fun privateModeScreenItemsTest() {
        homeScreen { }.dismissOnboarding()
        homeScreen { }.togglePrivateBrowsingMode()

        homeScreen {
            verifyHomeScreen()
            verifyNavigationToolbar()
            verifyHomePrivateBrowsingButton()
            verifyHomeMenu()
            verifyHomeWordmark()
            verifyTabButton()
            verifyPrivateSessionMessage()
            verifyHomeToolbar()
            verifyHomeComponent()
        }.openCommonMythsLink {
            verifyUrl("common-myths-about-private-browsing")
            mDevice.pressBack()
        }

        homeScreen {
            // To deal with the race condition where multiple "add tab" buttons are present,
            // we need to wait until previous HomeFragment View objects are gone.
            mDevice.waitNotNull(Until.gone(By.text(privateSessionMessage)), waitingTime)
            verifyHomeScreen()
            verifyNavigationToolbar()
            verifyHomePrivateBrowsingButton()
            verifyHomeMenu()
            verifyHomeWordmark()
            verifyTabButton()
            verifyPrivateSessionMessage()
            verifyHomeToolbar()
            verifyHomeComponent()
        }
    }

    @Test
    fun dismissOnboardingUsingSettingsTest() {
        homeScreen {
            verifyWelcomeHeader()
        }.openThreeDotMenu {
        }.openSettings {
            verifyGeneralHeading()
        }.goBack {
            verifyExistingTopSitesList()
        }
    }

    @Test
    fun dismissOnboardingUsingBookmarksTest() {
        homeScreen {
            verifyWelcomeHeader()
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarksMenuView()
            navigateUp()
        }
        homeScreen {
            verifyExistingTopSitesList()
        }
    }

    @Test
    fun dismissOnboardingUsingHelpTest() {
        featureSettingsHelper.setJumpBackCFREnabled(false)

        homeScreen {
            verifyWelcomeHeader()
        }.openThreeDotMenu {
        }.openHelp {
            verifyHelpUrl()
        }.goBack {
            verifyExistingTopSitesList()
        }
    }

    @Test
    fun toolbarTapDoesntDismissOnboardingTest() {
        homeScreen {
            verifyWelcomeHeader()
        }.openSearch {
            verifyScanButton()
            verifySearchEngineButton()
            verifyKeyboardVisibility()
        }.dismissSearchBar {
            verifyWelcomeHeader()
        }
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/fenix/issues/26932")
    @Test
    fun verifyCustomizeHomepageTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        featureSettingsHelper.setJumpBackCFREnabled(false)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.goToHomescreen {
        }.openCustomizeHomepage {
            clickJumpBackInButton()
            clickRecentBookmarksButton()
            clickRecentSearchesButton()
        }.goBack {
            verifyCustomizeHomepageButton(false)
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickJumpBackInButton()
        }.goBack {
            verifyCustomizeHomepageButton(true)
        }
    }
}
