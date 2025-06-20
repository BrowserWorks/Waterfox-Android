/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package net.waterfox.android.ui

import android.view.View
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.core.net.toUri
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.IntentReceiverActivity
import net.waterfox.android.R
import net.waterfox.android.components.components
import net.waterfox.android.customannotations.SmokeTest
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.AndroidAssetDispatcher
import net.waterfox.android.helpers.Constants
import net.waterfox.android.helpers.FeatureSettingsHelper
import net.waterfox.android.helpers.HomeActivityIntentTestRule
import net.waterfox.android.helpers.RecyclerViewIdlingResource
import net.waterfox.android.helpers.RetryTestRule
import net.waterfox.android.helpers.TestAssetHelper
import net.waterfox.android.helpers.TestHelper
import net.waterfox.android.helpers.TestHelper.appName
import net.waterfox.android.helpers.TestHelper.assertNativeAppOpens
import net.waterfox.android.helpers.TestHelper.createCustomTabIntent
import net.waterfox.android.helpers.TestHelper.generateRandomString
import net.waterfox.android.helpers.TestHelper.scrollToElementByText
import net.waterfox.android.helpers.ViewVisibilityIdlingResource
import net.waterfox.android.ui.robots.browserScreen
import net.waterfox.android.ui.robots.customTabScreen
import net.waterfox.android.ui.robots.enhancedTrackingProtection
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar
import net.waterfox.android.ui.robots.notificationShade
import net.waterfox.android.ui.robots.openEditURLView
import net.waterfox.android.ui.robots.searchScreen
import net.waterfox.android.ui.util.FRENCH_LANGUAGE_HEADER
import net.waterfox.android.ui.util.FRENCH_SYSTEM_LOCALE_OPTION
import net.waterfox.android.ui.util.ROMANIAN_LANGUAGE_HEADER
import net.waterfox.android.ui.util.STRING_ONBOARDING_TRACKING_PROTECTION_HEADER

/**
 * Test Suite that contains a part of the Smoke and Sanity tests defined in TestRail:
 * https://testrail.stage.mozaws.net/index.php?/suites/view/3192
 * Other smoke tests have been marked with the @SmokeTest annotation throughout the ui package in order to limit this class expansion.
 * These tests will verify different functionalities of the app as a way to quickly detect regressions in main areas
 */
@Suppress("ForbiddenComment")
@SmokeTest
class SmokeTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private var awesomeBar: ViewVisibilityIdlingResource? = null
    private var addonsListIdlingResource: RecyclerViewIdlingResource? = null
    private var recentlyClosedTabsListIdlingResource: RecyclerViewIdlingResource? = null
    private var readerViewNotification: ViewVisibilityIdlingResource? = null
    private var bookmarksListIdlingResource: RecyclerViewIdlingResource? = null
    private var localeListIdlingResource: RecyclerViewIdlingResource? = null
    private val customMenuItem = "TestMenuItem"
    private lateinit var browserStore: BrowserStore
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule(order = 0)
    val activityTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule()
    ) { it.activity }

    @get:Rule(order = 1)
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java, true, false
    )

    @Rule(order = 2)
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        // Initializing this as part of class construction, below the rule would throw a NPE
        // So we are initializing this here instead of in all related tests.
        browserStore = activityTestRule.activity.components.core.store

        // disabling the new homepage pop-up that interferes with the tests.
        featureSettingsHelper.setJumpBackCFREnabled(false)
        featureSettingsHelper.setTCPCFREnabled(false)

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()

        if (awesomeBar != null) {
            IdlingRegistry.getInstance().unregister(awesomeBar!!)
        }

        if (addonsListIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(addonsListIdlingResource!!)
        }

        if (recentlyClosedTabsListIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(recentlyClosedTabsListIdlingResource!!)
        }

        if (bookmarksListIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(bookmarksListIdlingResource!!)
        }

        if (readerViewNotification != null) {
            IdlingRegistry.getInstance().unregister(readerViewNotification)
        }

        if (localeListIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(localeListIdlingResource)
        }

        // resetting modified features enabled setting to default
        featureSettingsHelper.resetAllFeatureFlags()
    }

    // Verifies the first run onboarding screen
    @Test
    fun firstRunScreenTest() {
        homeScreen {
            verifyHomeScreen()
            verifyNavigationToolbar()
            verifyHomePrivateBrowsingButton()
            verifyHomeMenu()
            verifyHomeWordmark()

            verifyWelcomeHeader()
            // Sign in to Waterfox
            verifyStartSyncHeader()
            verifyAccountsSignInButton()

            // Always-on privacy
            scrollToElementByText(STRING_ONBOARDING_TRACKING_PROTECTION_HEADER)
            verifyAutomaticPrivacyHeader()
            verifyAutomaticPrivacyText()

            // Choose your theme
            verifyChooseThemeHeader()
            verifyChooseThemeText()
            verifyDarkThemeDescription()
            verifyDarkThemeToggle()
            verifyLightThemeDescription()
            verifyLightThemeToggle()

            // Pick your toolbar placement
            verifyTakePositionHeader()
            verifyTakePositionElements()

            // Your privacy
            verifyYourPrivacyHeader()
            verifyYourPrivacyText()
            verifyPrivacyNoticeButton()

            // Start Browsing
            verifyStartBrowsingButton()
        }
    }

    @Test
    // Verifies the functionality of the onboarding Start Browsing button
    fun startBrowsingButtonTest() {
        homeScreen {
            verifyStartBrowsingButton()
        }.clickStartBrowsingButton {
            verifySearchView()
        }
    }

    @Test
    /* Verifies the nav bar:
     - opening a web page
     - the existence of nav bar items
     - editing the url bar
     - the tab drawer button
     - opening a new search and dismissing the nav bar
    */
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun verifyBasicNavigationToolbarFunctionality() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                mDevice.waitForIdle()
                verifyNavURLBarItems()
            }.openNavigationToolbar {
            }.goBackToWebsite {
            }.openTabDrawer {
                verifyExistingTabList()
            }.openNewTab {
            }.dismissSearchBar {
                verifyHomeScreen()
            }
        }
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/fenix/issues/26711")
    @Test
    // Verifies the list of items in a tab's 3 dot menu
    fun verifyPageMainMenuItemsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            verifyPageThreeDotMainMenuItems()
        }
    }

    // Could be removed when more smoke tests from the History category are added
    @Test
    // Verifies the History menu opens from a tab's 3 dot menu
    fun openMainMenuHistoryItemTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists(activityTestRule)
        }
    }

    // Could be removed when more smoke tests from the Bookmarks category are added
    @Test
    // Verifies the Bookmarks menu opens from a tab's 3 dot menu
    fun openMainMenuBookmarksItemTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarksMenuView()
        }
    }

    @Test
    // Verifies the Add-ons menu opens from a tab's 3 dot menu
    fun openMainMenuAddonsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openAddonsManagerMenu {
            addonsListIdlingResource =
                RecyclerViewIdlingResource(
                    activityTestRule.activity.findViewById(R.id.add_ons_list),
                    1
                )
            IdlingRegistry.getInstance().register(addonsListIdlingResource!!)
            verifyAddonsItems()
            IdlingRegistry.getInstance().unregister(addonsListIdlingResource!!)
        }
    }

    @Test
    // Verifies the Synced tabs menu or Sync Sign In menu opens from a tab's 3 dot menu.
    // The test is assuming we are NOT signed in.
    fun openMainMenuSyncItemTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openSyncSignIn {
            verifyTurnOnSyncMenu()
        }
    }

    @Test
    // Test running on release builds in CI:
    // caution when making changes to it, so they don't block the builds
    // Verifies the Settings menu opens from a tab's 3 dot menu
    fun openMainMenuSettingsItemTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsView()
        }
    }

    @Test
    // Verifies the Find in page option in a tab's 3 dot menu
    fun openMainMenuFindInPageTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openFindInPage {
            verifyFindInPageSearchBarItems()
        }
    }

    @Test
    // Verifies the Add to home screen option in a tab's 3 dot menu
    fun mainMenuAddToHomeScreenTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val shortcutTitle = generateRandomString(5)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
        }.openThreeDotMenu {
            expandMenu()
        }.openAddToHomeScreen {
            clickCancelShortcutButton()
        }

        browserScreen {
        }.openThreeDotMenu {
            expandMenu()
        }.openAddToHomeScreen {
            verifyShortcutNameField("Test_Page_1")
            addShortcutName(shortcutTitle)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            verifyUrl(website.url.toString())
            verifyTabCounter("1")
        }
    }

    @Test
    // Verifies the Add to collection option in a tab's 3 dot menu
    fun openMainMenuAddToCollectionTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openSaveToCollection {
            verifyCollectionNameTextField()
        }
    }

    @Test
    // Verifies the Bookmark button in a tab's 3 dot menu
    fun mainMenuBookmarkButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.bookmarkPage {
            verifySnackBarText("Bookmark saved!")
        }
    }

    @Ignore("Failing with frequent ANR: https://github.com/mozilla-mobile/fenix/issues/25926")
    @Test
    // Device or AVD requires a Google Services Android OS installation with Play Store installed
    // Verifies the Open in app button when an app is installed
    fun mainMenuOpenInAppTest() {
        val playStoreUrl = "play.google.com/store/apps/details?id=net.waterfox.android"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(playStoreUrl.toUri()) {
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickOpenInApp {
            assertNativeAppOpens(Constants.PackageName.GOOGLE_PLAY_SERVICES, playStoreUrl)
        }
    }

    @Test
    // Verifies the Desktop site toggle in a tab's 3 dot menu
    fun mainMenuDesktopSiteTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.switchDesktopSiteMode {
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(true)
        }
    }

    @Test
    // Verifies the Share button in a tab's 3 dot menu
    fun mainMenuShareButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.clickShareButton {
            verifyShareTabLayout()
            verifySendToDeviceTitle()
            verifyShareALinkTitle()
        }
    }

    @Test
    // Verifies the refresh button in a tab's 3 dot menu
    fun mainMenuRefreshButtonTest() {
        val refreshWebPage = TestAssetHelper.getRefreshAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(refreshWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
            verifyThreeDotMenuExists()
        }.refreshPage {
            verifyPageContent("REFRESHED")
        }
    }

    @Ignore("Permanent failure: https://github.com/mozilla-mobile/fenix/issues/27312")
    @Test
    fun customTrackingProtectionSettingsTest() {
        val genericWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val trackingPage = TestAssetHelper.getEnhancedTrackingProtectionAsset(mockWebServer)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            verifyEnhancedTrackingProtectionOptionsEnabled()
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
        }.goBackToHomeScreen {}

        navigationToolbar {
            // browsing a basic page to allow GV to load on a fresh run
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {}

        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.openDetails {
            verifyTrackingCookiesBlocked()
            verifyCryptominersBlocked()
            verifyFingerprintersBlocked()
            verifyTrackingContentBlocked()
            viewTrackingContentBlockList()
        }
    }

    @Test
    // Verifies changing the default engine from the Search Shortcut menu
    fun selectSearchEnginesShortcutTest() {
        val enginesList = listOf("DuckDuckGo", "Google", "Amazon.com", "Wikipedia", "Bing", "eBay")

        for (searchEngine in enginesList) {
            homeScreen {
            }.openSearch {
                verifyKeyboardVisibility()
                clickSearchEngineShortcutButton()
                verifySearchEngineList(activityTestRule)
                changeDefaultSearchEngine(activityTestRule, searchEngine)
                verifySearchEngineIcon(searchEngine)
            }.submitQuery("mozilla ") {
                verifyUrl(searchEngine)
            }.goToHomescreen { }
        }
    }

    @Test
    // Swipes the nav bar left/right to switch between tabs
    fun swipeToSwitchTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            swipeNavBarRight(secondWebPage.url.toString())
            verifyUrl(firstWebPage.url.toString())
            swipeNavBarLeft(firstWebPage.url.toString())
            verifyUrl(secondWebPage.url.toString())
        }
    }

    @Test
    // Saves a login, then changes it and verifies the update
    fun updateSavedLoginTest() {
        val saveLoginTest =
            TestAssetHelper.getSaveLoginAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            verifySaveLoginPromptIsShown()
            // Click Save to save the login
            saveLoginFromPrompt("Save")
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            enterPassword("test")
            verifyUpdateLoginPromptIsShown()
            // Click Update to change the saved password
            saveLoginFromPrompt("Update")
        }.openThreeDotMenu {
        }.openSettings {
            TestHelper.scrollToElementByText("Logins and passwords")
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins {
            verifySecurityPromptForLogins()
            tapSetupLater()
            // Verify that the login appears correctly
            verifySavedLoginFromPrompt("test@example.com")
            viewSavedLoginDetails("test@example.com")
            revealPassword()
            verifyPasswordSaved("test") // failing here locally
        }
    }

    @Test
    // Verifies that a recently closed item is properly opened
    fun openRecentlyClosedItemTest() {
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
        }.clickRecentlyClosedItem("Test_Page_1", activityTestRule) {
            verifyUrl(website.url.toString())
        }
    }

    @Test
    // Verifies that tapping the "x" button removes a recently closed item from the list
    fun deleteRecentlyClosedTabsItemTest() {
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
            clickDeleteRecentlyClosedTabs(activityTestRule)
            verifyEmptyRecentlyClosedTabsList(activityTestRule)
        }
    }

    @Test
    // Verifies that deleting a Bookmarks folder also removes the item from inside it.
    fun deleteNonEmptyBookmarkFolderTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(website.url)
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarkTitle("Test_Page_1", activityTestRule)
            createFolder("My Folder", activityTestRule)
            verifyFolderTitle("My Folder", activityTestRule)
        }.openThreeDotMenu("Test_Page_1", activityTestRule) {
        }.clickEdit(activityTestRule) {
            clickParentFolderSelector(activityTestRule)
            selectFolder("My Folder", activityTestRule)
            navigateUp()
            saveEditBookmark()
            createFolder("My Folder 2", activityTestRule)
            verifyFolderTitle("My Folder 2", activityTestRule)
        }.openThreeDotMenu("My Folder 2", activityTestRule) {
        }.clickEdit(activityTestRule) {
            clickParentFolderSelector(activityTestRule)
            selectFolder("My Folder", activityTestRule)
            navigateUp()
            saveEditBookmark()
        }.openThreeDotMenu("My Folder", activityTestRule) {
        }.clickDelete(activityTestRule) {
            cancelFolderDeletion()
            verifyFolderTitle("My Folder", activityTestRule)
        }.openThreeDotMenu("My Folder", activityTestRule) {
        }.clickDelete(activityTestRule) {
            confirmDeletion()
            verifyDeleteSnackBarText()
            verifyBookmarkIsDeleted("My Folder", activityTestRule)
            verifyBookmarkIsDeleted("My Folder 2", activityTestRule)
            verifyBookmarkIsDeleted("Test_Page_1", activityTestRule)
            navigateUp()
        }

        browserScreen {
        }.openThreeDotMenu {
            verifyAddBookmarkButton()
        }
    }

    @Test
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun shareTabsFromTabsTrayTest() {
        val firstWebsite = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebsite = TestAssetHelper.getGenericAsset(mockWebServer, 2)
        val firstWebsiteTitle = firstWebsite.title
        val secondWebsiteTitle = secondWebsite.title
        val sharingApp = "Gmail"
        val sharedUrlsString = "${firstWebsite.url}\n\n${secondWebsite.url}"

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebsite.url) {
            verifyPageContent(firstWebsite.content)
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebsite.url.toString()) {
            verifyPageContent(secondWebsite.content)
        }.openTabDrawer {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openTabsListThreeDotMenu {
            verifyShareAllTabsButton()
        }.clickShareAllTabsButton {
            verifyShareTabsOverlay(firstWebsiteTitle, secondWebsiteTitle)
            verifySharingWithSelectedApp(
                sharingApp, sharedUrlsString,
                "$firstWebsiteTitle, $secondWebsiteTitle"
            )
        }
    }

    @Test
    fun emptyTabsTrayViewPrivateBrowsingTest() {
        navigationToolbar {
        }.openTabTray {
        }.toggleToPrivateTabs() {
            verifyNormalBrowsingButtonIsSelected(false)
            verifyPrivateBrowsingButtonIsSelected(true)
            verifySyncedTabsButtonIsSelected(false)
            verifyNoOpenTabsInPrivateBrowsing()
            verifyPrivateBrowsingNewTabButton()
            verifyTabTrayOverflowMenu(true)
            verifyEmptyTabsTrayMenuButtons()
        }
    }

    @Test
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun privateTabsTrayWithOpenedTabTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.togglePrivateBrowsingMode()

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
            verifyNormalBrowsingButtonIsSelected(false)
            verifyPrivateBrowsingButtonIsSelected(true)
            verifySyncedTabsButtonIsSelected(false)
            verifyTabTrayOverflowMenu(true)
            verifyTabsTrayCounter()
            verifyExistingTabList()
            verifyExistingOpenTabs(website.title)
            verifyCloseTabsButton(website.title)
            verifyOpenedTabThumbnail()
            verifyPrivateBrowsingNewTabButton()
        }
    }

    @Test
    // Test running on release builds in CI:
    // caution when making changes to it, so they don't block the builds
    fun noHistoryInPrivateBrowsingTest() {
        FeatureSettingsHelper().setTCPCFREnabled(false)
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.togglePrivateBrowsingMode()

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyEmptyHistoryView(activityTestRule)
        }
    }

    @Test
    fun addPrivateBrowsingShortcutTest() {
        homeScreen {
        }.dismissOnboarding()

        homeScreen {
        }.triggerPrivateBrowsingShortcutPrompt {
            verifyNoThanksPrivateBrowsingShortcutButton()
            verifyAddPrivateBrowsingShortcutButton()
            clickAddPrivateBrowsingShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut("Private $appName") {}
        searchScreen {
            verifySearchView()
        }.dismissSearchBar {
            verifyPrivateSessionMessage()
        }
    }

    @Test
    fun mainMenuInstallPWATest() {
        val pwaPage = "https://mozilla-mobile.github.io/testapp/"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(pwaPage.toUri()) {
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut("TEST_APP") {
            mDevice.waitForIdle()
            verifyNavURLBarHidden()
        }
    }

    @Test
    // Verifies that reader mode is detected and the custom appearance controls are displayed
    fun verifyReaderViewAppearanceUI() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)
        val estimatedReadingTime = "1 - 2 minutes"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        readerViewNotification = ViewVisibilityIdlingResource(
            activityTestRule.activity.findViewById(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions),
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
            verifyAppearanceColorGroup(true)
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }
    }

    @Test
    // Verifies the main menu of a custom tab with a custom menu item
    fun customTabMenuItemsTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem
            )
        )

        customTabScreen {
            verifyCustomTabCloseButton()
        }.openMainMenu {
            verifyPoweredByTextIsDisplayed()
            verifyCustomMenuItem(customMenuItem)
            verifyDesktopSiteButtonExists()
            verifyFindInPageButtonExists()
            verifyOpenInBrowserButtonExists()
            verifyBackButtonExists()
            verifyForwardButtonExists()
            verifyRefreshButtonExists()
        }
    }

    @Test
    // The test opens a link in a custom tab then sends it to the browser
    fun openCustomTabInBrowserTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString()
            )
        )

        customTabScreen {
            verifyCustomTabCloseButton()
        }.openMainMenu {
        }.clickOpenInBrowserButton {
            verifyTabCounter("1")
        }
    }

    @Ignore("Failing with frequent ANR: https://bugzilla.mozilla.org/show_bug.cgi?id=1764605")
    @Test
    fun audioPlaybackSystemNotificationTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(audioTestPage.url) {
            mDevice.waitForIdle()
            clickMediaPlayerPlayButton()
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists(audioTestPage.title)
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
            verifySystemNotificationGone(audioTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    @Test
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun tabMediaControlButtonTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(audioTestPage.url) {
            mDevice.waitForIdle()
            clickMediaPlayerPlayButton()
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openTabDrawer {
            verifyTabMediaControlButtonState("Pause")
            clickTabMediaControlButton("Pause")
            verifyTabMediaControlButtonState("Play")
        }.openTab(audioTestPage.title) {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }
    }

    @Test
    // For API>23
    // Verifies the default browser switch opens the system default apps menu.
    fun changeDefaultBrowserSetting() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyDefaultBrowserIsDisabled()
            clickDefaultBrowserSwitch()
            verifyAndroidDefaultAppsMenuAppears()
        }
        // Dismiss the request
        mDevice.pressBack()
    }

    @Ignore("Failing: https://github.com/mozilla-mobile/fenix/issues/26884")
    @Test
    fun copyTextTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            longClickAndCopyText("content")
        }.openNavigationToolbar {
            openEditURLView()
        }

        searchScreen {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText("content")
        }
    }

    @Ignore("Failing: https://github.com/mozilla-mobile/fenix/issues/26884")
    @Test
    fun selectAllAndCopyTextTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            longClickAndCopyText("content", true)
        }.openNavigationToolbar {
            openEditURLView()
        }

        searchScreen {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText("Page content: 1")
        }
    }

    @Test
    @Ignore("Waterfox: Not applicable. Currently no other languages than English are supported.")
    fun switchLanguageTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLanguageSubMenu {
            selectLanguage("Romanian", activityTestRule)
            verifyLanguageHeaderIsTranslated(ROMANIAN_LANGUAGE_HEADER)
            selectLanguage("Français", activityTestRule)
            verifyLanguageHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
            selectLanguage(FRENCH_SYSTEM_LOCALE_OPTION, activityTestRule)
            verifyLanguageHeaderIsTranslated("Language")
        }
    }

    @Test
    fun goToHomeScreenBottomToolbarTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            mDevice.waitForIdle()
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    @Test
    fun goToHomeScreenTopToolbarTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            clickTopToolbarToggle()
        }.goBack {
        }.goBack {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            mDevice.waitForIdle()
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    @Test
    fun goToHomeScreenBottomToolbarPrivateModeTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            togglePrivateBrowsingModeOnOff()
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            mDevice.waitForIdle()
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    @Test
    fun goToHomeScreenTopToolbarPrivateModeTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            togglePrivateBrowsingModeOnOff()
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            clickTopToolbarToggle()
        }.goBack {
        }.goBack {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            mDevice.waitForIdle()
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    @Test
    fun tabsSettingsMenuItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openTabsSubMenu {
            verifyTabViewOptions(activityTestRule)
            verifyCloseTabsOptions(activityTestRule)
            verifyMoveOldTabsToInactiveOptions(activityTestRule)
        }
    }
}
