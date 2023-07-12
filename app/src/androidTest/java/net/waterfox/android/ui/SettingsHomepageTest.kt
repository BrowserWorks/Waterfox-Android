/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestAssetHelper.getGenericAsset
import net.waterfox.android.helpers.TestHelper.restartApp
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar

/**
 *  Tests for verifying the Homepage settings menu
 *
 */
class SettingsHomepageTest {
    private lateinit var mockWebServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule(skipOnboarding = true)
    ) { it.activity }

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
        featureSettingsHelper.setJumpBackCFREnabled(false)
        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()

        // resetting modified features enabled setting to default
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun jumpBackInOptionTest() {
        val genericURL = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.goToHomescreen {
            verifyJumpBackInSectionIsDisplayed()
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickJumpBackInButton(composeTestRule)
        }.goBack {
            verifyJumpBackInSectionIsNotDisplayed()
        }
    }

    @SmokeTest
    @Test
    fun recentBookmarksOptionTest() {
        val genericURL = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openThreeDotMenu {
        }.bookmarkPage {
        }.goToHomescreen {
            verifyRecentBookmarksSectionIsDisplayed()
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickRecentBookmarksButton(composeTestRule)
        }.goBack {
            verifyRecentBookmarksSectionIsNotDisplayed()
        }
    }

    @SmokeTest
    @Test
    fun startOnHomepageTest() {
        val genericURL = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openThreeDotMenu {
        }.openSettings {
        }.openHomepageSubMenu {
            clickStartOnHomepageButton(composeTestRule)
        }

        restartApp(composeTestRule.activityRule)

        homeScreen {
            verifyHomeScreen()
        }
    }

    @SmokeTest
    @Test
    fun startOnLastTabTest() {
        val firstWebPage = getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openHomepageSubMenu {
            clickStartOnHomepageButton(composeTestRule)
        }

        restartApp(composeTestRule.activityRule)

        homeScreen {
            verifyHomeScreen()
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.goToHomescreen {
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickStartOnLastTabButton(composeTestRule)
        }

        restartApp(composeTestRule.activityRule)

        browserScreen {
            verifyUrl(firstWebPage.url.toString())
        }
    }

    @Ignore("Intermittent test: https://github.com/mozilla-mobile/fenix/issues/26559")
    @SmokeTest
    @Test
    fun setWallpaperTest() {
        val wallpapers = listOf(
            "Wallpaper Item: amethyst",
            "Wallpaper Item: cerulean",
            "Wallpaper Item: sunrise"
        )

        for (wallpaper in wallpapers) {
            homeScreen {
            }.openThreeDotMenu {
            }.openCustomizeHome {
                openWallpapersMenu()
                selectWallpaper(wallpaper)
                verifySnackBarText("Wallpaper updated!")
            }.clickSnackBarViewButton {
                verifyWallpaperImageApplied(true)
            }
        }
    }
}
