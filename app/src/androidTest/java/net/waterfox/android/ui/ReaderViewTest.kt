/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import android.view.View
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.R
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.helpers.ViewVisibilityIdlingResource
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of content context menus
 *
 *  - Verifies Reader View entry and detection when available UI and functionality
 *  - Verifies Reader View exit UI and functionality
 *  - Verifies Reader View appearance controls UI and functionality
 *
 */

class ReaderViewTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice
    private var readerViewNotification: ViewVisibilityIdlingResource? = null
    private val estimatedReadingTime = "1 - 2 minutes"

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
        IdlingRegistry.getInstance().unregister(readerViewNotification)
    }

    /**
     *  Verify that Reader View capable pages
     *
     *   - Show the toggle button in the navigation bar
     *
     */
    @Test
    fun verifyReaderViewPageMenuDetection() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityIntentTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
            View.VISIBLE
        )

        IdlingRegistry.getInstance().register(readerViewNotification)

        navigationToolbar {
            verifyReaderViewDetected(true)
        }
    }

    /**
     *  Verify that non Reader View capable pages
     *
     *   - Reader View toggle should not be visible in the navigation toolbar
     *
     */
    @Test
    fun verifyNonReaderViewPageMenuNoDetection() {
        var genericPage =
            TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            mDevice.waitForIdle()
        }

        navigationToolbar {
            verifyReaderViewDetected(false)
        }
    }

    @Test
    fun verifyReaderViewToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityIntentTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
            View.VISIBLE
        )

        IdlingRegistry.getInstance().register(readerViewNotification)

        navigationToolbar {
            verifyReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
        }
        browserScreen {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.closeBrowserMenuToBrowser { }

        navigationToolbar {
            verifyCloseReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
            verifyReaderViewDetected(true)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(false)
        }.close { }
    }

    @Test
    fun verifyReaderViewAppearanceFontToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityIntentTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
            View.VISIBLE
        )

        IdlingRegistry.getInstance().register(readerViewNotification)

        navigationToolbar {
            verifyReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
        }

        browserScreen {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontGroup(true)
            verifyAppearanceFontSansSerif(true)
            verifyAppearanceFontSerif(true)
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
        }.toggleSansSerif {
            verifyAppearanceFontIsActive("SANSSERIF")
        }.toggleSerif {
            verifyAppearanceFontIsActive("SERIF")
        }
    }

    @Test
    fun verifyReaderViewAppearanceFontSizeToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityIntentTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
            View.VISIBLE
        )

        IdlingRegistry.getInstance().register(readerViewNotification)

        navigationToolbar {
            verifyReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
        }

        browserScreen {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
            verifyAppearanceFontSize(3)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(6)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(3)
        }
    }

    @Test
    fun verifyReaderViewAppearanceColorSchemeChange() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityIntentTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
            View.VISIBLE
        )

        IdlingRegistry.getInstance().register(readerViewNotification)

        navigationToolbar {
            verifyReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
        }

        browserScreen {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }.toggleColorSchemeChangeDark {
            verifyAppearanceColorSchemeChange("DARK")
        }.toggleColorSchemeChangeSepia {
            verifyAppearanceColorSchemeChange("SEPIA")
        }.toggleColorSchemeChangeLight {
            verifyAppearanceColorSchemeChange("LIGHT")
        }
    }
}
