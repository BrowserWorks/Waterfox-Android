/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.ui.robots.DeepLinkRobot

/**
 *  Tests for verifying basic functionality of deep links
 *  - waterfox://home
 *  - waterfox://open
 *  - waterfox://settings_notifications — take the user to the notification settings page
 *  - waterfox://settings_privacy — take the user to the privacy settings page.
 *  - waterfox://settings_search_engine — take the user to the search engine page, to set the default search engine.
 *  - waterfox://home_collections — take the user to the home screen to see the list of collections.
 *  - waterfox://urls_history — take the user to the history list.
 *  - waterfox://urls_bookmarks — take the user to the bookmarks list
 *  - waterfox://settings_logins — take the user to the settings page to do with logins (not the saved logins).
 **/

@Ignore("All tests perma-failing, see: https://github.com/mozilla-mobile/fenix/issues/13491")
class DeepLinkTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer

    private val robot = DeepLinkRobot()

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule()

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
    fun openHomeScreen() {
        robot.openHomeScreen {
            verifyHomeComponent()
        }
        robot.openSettings { /* move away from the home screen */ }
        robot.openHomeScreen {
            verifyHomeComponent()
        }
    }

    @Test
    fun openURL() {
        val genericURL =
            TestAssetHelper.getGenericAsset(mockWebServer, 1)
        robot.openURL(genericURL.url.toString()) {
            verifyUrl(genericURL.url.toString())
        }
    }

    @Test
    fun openBookmarks() {
        robot.openBookmarks {
            // verify we can see headings.
            verifyFolderTitle("Desktop Bookmarks")
        }
    }

    @Test
    fun openHistory() {
        robot.openHistory {
            verifyHistoryMenuView()
        }
    }

    @Test
    fun openCollections() {
        robot.openHomeScreen { /* do nothing */ }.dismissOnboarding()
        robot.openCollections {
            verifyCollectionsHeader()
        }
    }

    @Test
    fun openSettings() {
        robot.openSettings {
            verifyGeneralHeading()
            verifyAdvancedHeading()
        }
    }

    @Test
    fun openSettingsLogins() {
        robot.openSettingsLogins {
            verifyDefaultView()
            verifyDefaultValueAutofillLogins(InstrumentationRegistry.getInstrumentation().targetContext)
        }
    }

    @Test
    fun openSettingsPrivacy() {
        robot.openSettingsPrivacy {
            verifyPrivacyHeading()
        }
    }

    @Test
    fun openSettingsTrackingProtection() {
        robot.openSettingsTrackingProtection {
            verifyEnhancedTrackingProtectionHeader()
        }
    }

    @Ignore("Crashing, see: https://github.com/mozilla-mobile/fenix/issues/11239")
    @Test
    fun openSettingsSearchEngine() {
        robot.openSettingsSearchEngine {
            verifyDefaultSearchEngineHeader()
        }
    }

    @Test
    fun openSettingsNotifications() {
        robot.openSettingsNotification {
            verifyNotifications()
        }
    }

    @Test
    fun openMakeDefaultBrowser() {
        robot.openMakeDefaultBrowser {
            verifyMakeDefaultBrowser()
        }
    }
}
