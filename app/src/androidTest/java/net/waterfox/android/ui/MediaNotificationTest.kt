/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar
import net.waterfox.android.ui.robots.notificationShade

/**
 *  Tests for verifying basic functionality of media notifications:
 *  - video and audio playback system notifications appear and can pause/play the media content
 *  - a media notification icon is displayed on the homescreen for the tab playing media content
 *  Note: this test only verifies media notifications, not media itself
 */
class MediaNotificationTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice

    @get:Rule
    val activityTestRule = HomeActivityTestRule()
    private lateinit var browserStore: BrowserStore

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        // Initializing this as part of class construction, below the rule would throw a NPE
        // So we are initializing this here instead of in all tests.
        browserStore = activityTestRule.activity.components.core.store

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

    @Ignore("Failing with ANR: https://github.com/mozilla-mobile/fenix/issues/15754")
    @Test
    fun videoPlaybackSystemNotificationTest() {
        val videoTestPage = TestAssetHelper.getVideoPageAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(videoTestPage.url) {
            mDevice.waitForIdle()
            clickMediaPlayerPlayButton()
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists(videoTestPage.title)
            clickMediaNotificationControlButton("Pause")
            verifyMediaSystemNotificationButtonState("Play")
        }

        mDevice.pressBack()

        browserScreen {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }.openTabDrawer {
            closeTab()
        }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationGone(videoTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    @Ignore("Failing with frequent ANR: https://bugzilla.mozilla.org/show_bug.cgi?id=1764605")
    @Test
    fun mediaSystemNotificationInPrivateModeTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        navigationToolbar {
        }.openTabTray {
        }.toggleToPrivateTabs {
        }.openNewTab {
        }.submitQuery(audioTestPage.url.toString()) {
            mDevice.waitForIdle()
            clickMediaPlayerPlayButton()
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists("A site is playing media")
            clickMediaNotificationControlButton("Pause")
            verifyMediaSystemNotificationButtonState("Play")
        }

        mDevice.pressBack()

        browserScreen {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }.openTabDrawer {
            closeTab()
            verifySnackBarText("Private tab closed")
        }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationGone("A site is playing media")
        }

        // close notification shade before and go back to regular mode before the next test
        mDevice.pressBack()
        homeScreen { }.togglePrivateBrowsingMode()
    }
}
