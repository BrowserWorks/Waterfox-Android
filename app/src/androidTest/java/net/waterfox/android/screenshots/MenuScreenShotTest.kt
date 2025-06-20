/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package net.waterfox.android.screenshots

import android.os.SystemClock
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.waterfox.android.R
import net.waterfox.android.helpers.*
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.ext.waitNotNull
import net.waterfox.android.ui.robots.bookmarksMenu
import net.waterfox.android.ui.robots.homeScreen
import net.waterfox.android.ui.robots.navigationToolbar
import net.waterfox.android.ui.robots.swipeToBottom
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class MenuScreenShotTest : ScreenshotTest() {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice
    private val featureSettingsHelper = FeatureSettingsHelper()

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

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

        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        featureSettingsHelper.resetAllFeatureFlags()
        activity.finishAndRemoveTask()
        mockWebServer.shutdown()
    }

    @Test
    fun threeDotMenuTest() {
        homeScreen {
        }.openThreeDotMenu {
            Screengrab.screenshot("ThreeDotMenuMainRobot_three-dot-menu")
        }
    }

    @Test
    fun settingsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            Screengrab.screenshot("SettingsRobot_settings-menu")
        }.openTurnOnSyncMenu {
            Screengrab.screenshot("AccountSettingsRobot_settings-account")
        }.goBack {
        }.openSearchSubMenu {
            Screengrab.screenshot("SettingsSubMenuSearchRobot_settings-search")
        }.goBack {
        }.openCustomizeSubMenu {
            Screengrab.screenshot("SettingsSubMenuThemeRobot_settings-theme")
        }.goBack {
        }.openAccessibilitySubMenu {
            Screengrab.screenshot("SettingsSubMenuAccessibilityRobot_settings-accessibility")
        }.goBack {
        }.openLanguageSubMenu {
            Screengrab.screenshot("SettingsSubMenuAccessibilityRobot_settings-language")
        }.goBack {
            // From about here we need to scroll up to ensure all settings options are visible.
        }.openSetDefaultBrowserSubMenu {
            Screengrab.screenshot("SettingsSubMenuDefaultBrowserRobot_settings-default-browser")
        }.goBack {
            // Disabled for Pixel 2
            // }.openEnhancedTrackingProtectionSubMenu {
            //     Screengrab.screenshot("settings-enhanced-tp")
            // }.goBack {
        }.openLoginsAndPasswordSubMenu {
            Screengrab.screenshot("SettingsSubMenuLoginsAndPasswords-settings-logins-passwords")
        }.goBack {
            swipeToBottom()
            Screengrab.screenshot("SettingsRobot_settings-scroll-to-bottom")
        }.openAddonsManagerMenu {
            Screengrab.screenshot("settings-addons")
        }
    }

    @Test
    fun historyTest() {
        homeScreen {
        }.openThreeDotMenu {
        }
        openHistoryThreeDotMenu()
        Screengrab.screenshot("HistoryRobot_history-menu")
    }

    @Test
    fun bookmarksManagementTest() {
        homeScreen {
        }.openThreeDotMenu {
        }
        openBookmarksThreeDotMenu()
        Screengrab.screenshot("BookmarksRobot_bookmarks-menu")
        bookmarksMenu {
            clickAddFolderButtonUsingId()
            Screengrab.screenshot("BookmarksRobot_add-folder-view")
            saveNewFolder()
            Screengrab.screenshot("BookmarksRobot_error-empty-folder-name")
            addNewFolderName("test", composeTestRule)
            saveNewFolder()
        }.openThreeDotMenu("test", composeTestRule) {
            Screengrab.screenshot("ThreeDotMenuBookmarksRobot_folder-menu")
        }
        editBookmarkFolder()
        Screengrab.screenshot("ThreeDotMenuBookmarksRobot_edit-bookmark-folder-menu")
        // It may be needed to wait here to have the screenshot
        bookmarksMenu {
            navigateUp()
        }.openThreeDotMenu("test", composeTestRule) {
            deleteBookmarkFolder()
            Screengrab.screenshot("ThreeDotMenuBookmarksRobot_delete-bookmark-folder-menu")
        }
    }

    @Test
    fun collectionMenuTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        navigationToolbar {
            Screengrab.screenshot("NavigationToolbarRobot_navigation-toolbar")
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            Screengrab.screenshot("BrowserRobot_enter-url")
        }.openTabDrawer {
            TestAssetHelper.waitingTime
            Screengrab.screenshot("TabDrawerRobot_one-tab-open")
        }.openTabsListThreeDotMenu {
            TestAssetHelper.waitingTime
            Screengrab.screenshot("TabDrawerRobot_three-dot-menu")
        }
    }

    @Test
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun tabMenuTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            Screengrab.screenshot("TabDrawerRobot_browser-tab-menu")
        }.closeBrowserMenuToBrowser {
        }.openTabDrawer {
            Screengrab.screenshot("TabDrawerRobot_tab-drawer-with-tabs")
            closeTab()
            TestAssetHelper.waitingTime
            Screengrab.screenshot("TabDrawerRobot_remove-tab")
        }
    }

    @Test
    fun saveLoginPromptTest() {
        val saveLoginTest =
            TestAssetHelper.getSaveLoginAsset(mockWebServer)
        navigationToolbar {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            verifySaveLoginPromptIsShownNotSave()
            SystemClock.sleep(TestAssetHelper.waitingTimeShort)
            Screengrab.screenshot("save-login-prompt")
        }
    }
}

fun openHistoryThreeDotMenu() = onView(withText(R.string.library_history)).click()

fun openBookmarksThreeDotMenu() = onView(withText(R.string.library_bookmarks)).click()

fun editBookmarkFolder() = onView(withText(R.string.bookmark_menu_edit_button)).click()

fun deleteBookmarkFolder() = onView(withText(R.string.bookmark_menu_delete_button)).click()

fun tapOnTabCounter() = onView(withId(mozilla.components.ui.tabcounter.R.id.counter_text)).click()

fun settingsAccountPreferences() = onView(withText(R.string.preferences_sync_2)).click()

fun settingsSearch() = onView(withText(R.string.preferences_search)).click()

fun settingsTheme() = onView(withText(R.string.preferences_customize)).click()

fun settingsAccessibility() = onView(withText(R.string.preferences_accessibility)).click()

fun settingDefaultBrowser() = onView(withText(R.string.preferences_set_as_default_browser)).click()

fun settingsToolbar() = onView(withText(R.string.preferences_toolbar)).click()

fun settingsTP() = onView(withText(R.string.preference_enhanced_tracking_protection)).click()

fun settingsAddToHomeScreen() = onView(withText(R.string.preferences_add_private_browsing_shortcut)).click()

fun settingsRemoveData() = onView(withText(R.string.preferences_delete_browsing_data)).click()

fun loginsAndPassword() = onView(withText(R.string.preferences_passwords_logins_and_passwords)).click()

fun addOns() = onView(withText(R.string.preferences_addons)).click()

fun settingsLanguage() = onView(withText(R.string.preferences_language)).click()

fun verifySaveLoginPromptIsShownNotSave() {
    mDevice.waitNotNull(Until.findObjects(By.text("test@example.com")), TestAssetHelper.waitingTime)
    val submitButton = mDevice.findObject(By.res("submit"))
    submitButton.clickAndWait(Until.newWindow(), TestAssetHelper.waitingTime)
}

fun clickAddFolderButtonUsingId() = onView(withId(R.id.add_bookmark_folder)).click()
