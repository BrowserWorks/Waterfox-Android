/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import net.waterfox.android.R
import net.waterfox.android.helpers.Constants
import net.waterfox.android.helpers.Constants.LONG_CLICK_DURATION
import net.waterfox.android.helpers.Constants.SPEECH_RECOGNITION
import net.waterfox.android.helpers.SessionLoadedIdlingResource
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestAssetHelper.waitingTimeShort
import net.waterfox.android.helpers.TestHelper.isPackageInstalled
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.TestHelper.waitForObjects
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the search fragment.
 */
class SearchRobot {
    fun verifySearchView() = assertSearchView()
    fun verifyBrowserToolbar() = assertBrowserToolbarEditView()
    fun verifyScanButton() = assertScanButton()

    fun verifyVoiceSearchButtonVisibility(enabled: Boolean) {
        if (enabled) {
            assertTrue(voiceSearchButton.waitForExists(waitingTime))
        } else {
            assertFalse(voiceSearchButton.waitForExists(waitingTime))
        }
    }

    // Device or AVD requires a Google Services Android OS installation
    fun startVoiceSearch() {
        voiceSearchButton.click()

        // Accept runtime permission (API 30) for Google Voice
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            val allowPermission = mDevice.findObject(
                UiSelector().text(
                    when {
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.R -> "Allow all the time"
                        else -> "While using the app"
                    }
                )
            )

            if (allowPermission.exists()) {
                allowPermission.click()
            }

            if (isPackageInstalled(Constants.PackageName.GOOGLE_QUICK_SEARCH)) {
                Intents.intended(IntentMatchers.hasAction(SPEECH_RECOGNITION))
            }
        }
    }

    fun verifySearchEngineButton() = assertSearchButton()
    fun verifySearchWithText() = assertSearchWithText()
    fun verifySearchEngineResults(rule: ComposeTestRule, searchSuggestion: String, searchEngineName: String) =
        assertSearchEngineResults(rule, searchSuggestion, searchEngineName)
    fun verifySearchEngineSuggestionResults(rule: ComposeTestRule, searchSuggestion: String) =
        assertSearchEngineSuggestionResults(rule, searchSuggestion)
    fun verifyNoSuggestionsAreDisplayed(rule: ComposeTestRule, searchSuggestion: String) =
        assertNoSuggestionsAreDisplayed(rule, searchSuggestion)
    fun verifySearchSettings() = assertSearchSettings()
    fun verifySearchBarEmpty() = assertSearchBarEmpty()

    fun verifyKeyboardVisibility() = assertKeyboardVisibility(isExpectedToBeVisible = true)
    fun verifySearchEngineList(rule: ComposeTestRule) = rule.assertSearchEngineList()
    fun verifySearchEngineIcon(expectedText: String) {
        onView(withContentDescription(expectedText))
    }
    fun verifyDefaultSearchEngine(expectedText: String) = assertDefaultSearchEngine(expectedText)

    fun verifyEnginesListShortcutContains(rule: ComposeTestRule, searchEngineName: String) = assertEngineListShortcutContains(rule, searchEngineName)

    fun changeDefaultSearchEngine(rule: ComposeTestRule, searchEngineName: String) =
        rule.selectDefaultSearchEngine(searchEngineName)

    fun clickSearchEngineShortcutButton() {
        val searchEnginesShortcutButton = mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/search_engines_shortcut_button")
        )
        searchEnginesShortcutButton.waitForExists(waitingTime)
        searchEnginesShortcutButton.click()
    }

    fun clickScanButton() {
        scanButton.waitForExists(waitingTime)
        scanButton.click()
    }

    fun clickDismissPermissionRequiredDialog() {
        dismissPermissionButton.waitForExists(waitingTime)
        dismissPermissionButton.click()
    }

    fun clickGoToPermissionsSettings() {
        goToPermissionsSettingsButton.waitForExists(waitingTime)
        goToPermissionsSettingsButton.click()
    }

    fun verifyScannerOpen() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/view_finder"))
                .waitForExists(waitingTime)
        )
    }

    fun typeSearch(searchTerm: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
        ).waitForExists(waitingTime)

        browserToolbarEditView().setText(searchTerm)

        mDevice.waitForIdle()
    }

    fun clickSearchEngineButton(rule: ComposeTestRule, searchEngineName: String) {
        rule.waitForIdle()

        mDevice.waitForObjects(
            mDevice.findObject(
                UiSelector().textContains(searchEngineName)
            )
        )

        rule.onNodeWithText(searchEngineName)
            .assertExists()
            .assertHasClickAction()
            .performClick()
    }

    fun clickSearchEngineResult(rule: ComposeTestRule, searchSuggestion: String) {
        mDevice.waitNotNull(
            Until.findObjects(By.text(searchSuggestion)),
            waitingTime
        )

        rule.onNodeWithText(searchSuggestion)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToSearchEngineSettings(rule: ComposeTestRule) {
        // Soft keyboard is visible on screen on view access; hide it
        onView(allOf(withId(R.id.search_wrapper))).perform(
            closeSoftKeyboard()
        )

        mDevice.findObject(UiSelector().text("Google"))
            .waitForExists(waitingTime)

        rule.onNodeWithTag("mozac.awesomebar.suggestions")
            .performScrollToIndex(5)
    }

    fun clickSearchEngineSettings(rule: ComposeTestRule) {
        rule.onNodeWithText("Search engine settings")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
    }

    fun clickClearButton() {
        clearButton().click()
    }

    fun longClickToolbar() {
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/awesomeBar"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
            .waitForExists(waitingTime)
        val toolbar = mDevice.findObject(By.res("$packageName:id/toolbar"))
        toolbar.click(LONG_CLICK_DURATION)
    }

    fun clickPasteText() {
        mDevice.findObject(UiSelector().textContains("Paste")).waitForExists(waitingTime)
        val pasteText = mDevice.findObject(By.textContains("Paste"))
        pasteText.click()
    }

    fun expandSearchSuggestionsList() {
        awesomeBar.swipeUp(2)
    }

    fun verifyPastedToolbarText(expectedText: String) = assertPastedToolbarText(expectedText)

    class Transition {
        private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

        fun dismissSearchBar(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle()
            closeSoftKeyboard()
            mDevice.pressBack()
            try {
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            } catch (e: AssertionError) {
                mDevice.pressBack()
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            }

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            browserToolbarEditView().setText("mozilla\n")
            mDevice.pressEnter()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun submitQuery(query: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            sessionLoadedIdlingResource = SessionLoadedIdlingResource()
            searchWrapper().waitForExists(waitingTime)
            browserToolbarEditView().setText(query)
            mDevice.pressEnter()

            runWithIdleRes(sessionLoadedIdlingResource) {
                assertTrue(
                    mDevice.findObject(
                        UiSelector().resourceId("$packageName:id/browserLayout")
                    ).waitForExists(waitingTime)
                )
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goToSearchEngine(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }
    }
}

private fun browserToolbarEditView() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))

private val dismissPermissionButton =
    mDevice.findObject(UiSelector().text("DISMISS"))

private val goToPermissionsSettingsButton =
    mDevice.findObject(UiSelector().text("GO TO SETTINGS"))

private val scanButton =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/qr_scan_button"))

private fun clearButton() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_clear_view"))

private fun searchWrapper() = mDevice.findObject(UiSelector().resourceId("$packageName:id/search_wrapper"))

private fun assertSearchEngineResults(rule: ComposeTestRule, searchSuggestion: String, searchEngineName: String) {
    rule.waitUntil(waitingTime, waitForSearchSuggestions(rule, searchSuggestion, searchEngineName))
    rule.onNodeWithText(searchSuggestion).assertIsDisplayed()
}

private fun waitForSearchSuggestions(rule: ComposeTestRule, searchSuggestion: String, searchEngineName: String): () -> Boolean =
    {
        rule.waitForIdle()
        mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains(searchSuggestion)))
        rule.onAllNodesWithTag("mozac.awesomebar.suggestion").assertAny(hasText(searchSuggestion) and hasText(searchEngineName))
        mDevice.findObject(UiSelector().textContains(searchSuggestion)).waitForExists(waitingTime)
    }

private fun assertSearchEngineSuggestionResults(rule: ComposeTestRule, searchResult: String) {
    rule.waitForIdle()

    assertTrue(
        mDevice.findObject(UiSelector().textContains(searchResult))
            .waitForExists(waitingTime)
    )
}

private fun assertNoSuggestionsAreDisplayed(rule: ComposeTestRule, searchTerm: String) {
    rule.waitForIdle()

    assertFalse(
        mDevice.findObject(UiSelector().textContains(searchTerm))
            .waitForExists(waitingTime)
    )
}

private fun assertSearchView() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/search_wrapper")
        ).waitForExists(waitingTime)
    )

private fun assertBrowserToolbarEditView() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
        ).waitForExists(waitingTime)
    )

private fun assertScanButton() =
    assertTrue(
        scanButton.waitForExists(waitingTime)
    )

private fun assertSearchButton() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/search_engines_shortcut_button")
        ).waitForExists(waitingTime)
    )

private fun assertSearchWithText() =
    onView(allOf(withText("THIS TIME, SEARCH WITH:")))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchSettings() =
    onView(allOf(withText("Default search engine")))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchBarEmpty() =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
                .textContains("")
        ).waitForExists(waitingTime)
    )

fun searchScreen(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
    SearchRobot().interact()
    return SearchRobot.Transition()
}

private fun assertKeyboardVisibility(isExpectedToBeVisible: Boolean): () -> Unit = {
    searchWrapper().waitForExists(waitingTime)

    assertEquals(
        "Keyboard not shown",
        isExpectedToBeVisible,
        mDevice
            .executeShellCommand("dumpsys input_method | grep mInputShown")
            .contains("mInputShown=true")
    )
}

private fun ComposeTestRule.assertSearchEngineList() {
    onView(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_edit_icon)).click()

    onNodeWithText("Google")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Amazon.com")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Bing")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("DuckDuckGo")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Wikipedia")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("eBay")
        .assertExists()
        .assertIsDisplayed()
}

@OptIn(ExperimentalTestApi::class)
private fun assertEngineListShortcutContains(rule: ComposeTestRule, searchEngineName: String) {
    try {
        rule.waitForIdle()
    } catch (e: ComposeNotIdleException) {
        mDevice.pressBack()
        navigationToolbar {
        }.clickUrlbar {
            clickSearchEngineShortcutButton()
        }
    } finally {
        mDevice.findObject(
            UiSelector().textContains("Google")
        ).waitForExists(waitingTime)

        rule.onNodeWithTag("mozac.awesomebar.suggestions")
            .performScrollToIndex(5)

        rule.onNodeWithText(searchEngineName)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}

private fun ComposeTestRule.selectDefaultSearchEngine(searchEngine: String) {
    onView(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_edit_icon)).click()

    onNodeWithText(searchEngine)
        .assertExists()
        .assertIsDisplayed()
        .performClick()
}

private fun assertDefaultSearchEngine(expectedText: String) =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_edit_icon")
                .descriptionContains(expectedText)
        ).waitForExists(waitingTime)
    )

private fun assertPastedToolbarText(expectedText: String) {
    mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
        .waitForExists(waitingTime)
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_url_view"))
        .waitForExists(waitingTime)
    onView(
        allOf(
            withSubstring(expectedText),
            withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_edit_url_view)
        )
    ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private val awesomeBar =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))

private val voiceSearchButton = mDevice.findObject(UiSelector().description("Voice search"))

private val searchSuggestionsList =
    UiScrollable(
        UiSelector().className("android.widget.ScrollView")
    )
