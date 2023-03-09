/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.ext.settings
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.ui.robots.historyMenu
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.multipleSelectionToolbar
import net.waterfox.android.ui.robots.navigationToolbar
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 *  Tests for verifying basic functionality of history
 *
 */
class HistoryTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        HomeActivityTestRule()
    ) { it.activity }

    private val activity by lazy { composeTestRule.activity }

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        activity.settings().shouldShowJumpBackInCFR = false

        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
        // Clearing all history data after each test to avoid overlapping data
        val historyStorage = PlacesHistoryStorage(activity.applicationContext)
        runBlocking {
            historyStorage.deleteEverything()
        }
    }

    @Test
    fun noHistoryItemsInCacheTest() {
        homeScreen {
        }.openThreeDotMenu {
            verifyHistoryButton()
        }.openHistory {
            verifyHistoryMenuView()
            verifyEmptyHistoryView(composeTestRule)
        }
    }

    @Test
    // Test running on release builds in CI:
    // caution when making changes to it, so they don't block the builds
    fun visitedUrlHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            verifyHistoryMenuView()
            verifyVisitedTimeTitle(composeTestRule)
            verifyFirstTestPageTitle(composeTestRule)
            verifyTestPageUrl(firstWebPage.url, composeTestRule)
        }
    }

    @Test
    fun deleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            clickDeleteHistoryButton(firstWebPage.url.toString(), composeTestRule)
            verifyDeleteSnackbarText("Deleted")
            // TODO: [Waterfox] fix this
//            verifyEmptyHistoryView(composeTestRule)
        }
    }

    @Test
    fun undoDeleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            clickDeleteHistoryButton(firstWebPage.url.toString(), composeTestRule)
            verifyUndoDeleteSnackBarButton()
            clickUndoDeleteButton()
            verifyHistoryItemExists(true, firstWebPage.url.toString(), composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun cancelDeleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            clickDeleteAllHistoryButton()
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            cancelDeleteHistory()
            verifyHistoryItemExists(true, firstWebPage.url.toString(), composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun deleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            clickDeleteAllHistoryButton()
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            confirmDeleteAllHistory()
            verifyDeleteSnackbarText("Browsing data deleted")
            // TODO: [Waterfox] fix this
//            verifyEmptyHistoryView(composeTestRule)
        }
    }

    @SmokeTest
    @Test
    fun historyMultiSelectionToolbarItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            longTapSelectItem(firstWebPage.url, composeTestRule)
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark(composeTestRule)
            verifyMultiSelectionCounter()
            verifyShareHistoryButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToHistory {
            verifyHistoryMenuView()
        }
    }

    @Test
    fun openHistoryInNewTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
            closeTab()
        }

        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            longTapSelectItem(firstWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyExistingTabList()
            verifyNormalModeSelected()
        }
    }

    @Test
    fun openHistoryInPrivateTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            longTapSelectItem(firstWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyExistingTabList()
            verifyPrivateModeSelected()
        }
    }

    @Test
    fun deleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            mDevice.waitForIdle()
            verifyUrl(secondWebPage.url.toString())
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            verifyHistoryItemExists(true, firstWebPage.url.toString(), composeTestRule)
            verifyHistoryItemExists(true, secondWebPage.url.toString(), composeTestRule)
            longTapSelectItem(firstWebPage.url, composeTestRule)
            tapSelectItem(secondWebPage.url, composeTestRule)
            openActionBarOverflowOrOptionsMenu(activity)
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        historyMenu {
            // TODO: [Waterfox] fix this
//            verifyEmptyHistoryView(composeTestRule)
        }
    }

    @Test
    fun shareButtonTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(composeTestRule)
            longTapSelectItem(firstWebPage.url, composeTestRule)
        }

        multipleSelectionToolbar {
            clickShareHistoryButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    @Test
    fun verifyRecentlyClosedTabsListTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
            closeTab()
        }.openTabDrawer {
        }.openRecentlyClosedTabs {
            verifyRecentlyClosedTabsMenuView()
            verifyRecentlyClosedTabsPageTitle("Test_Page_1", composeTestRule)
            verifyRecentlyClosedTabsUrl(website.url, composeTestRule)
        }
    }

}
