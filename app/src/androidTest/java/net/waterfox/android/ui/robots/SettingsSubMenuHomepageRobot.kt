/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ui.robots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.uiautomator.UiSelector
import net.waterfox.android.R
import net.waterfox.android.helpers.TestAssetHelper.waitingTimeShort
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.click
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Assert.assertTrue

/**
 * Implementation of Robot Pattern for the settings Homepage sub menu.
 */
class SettingsSubMenuHomepageRobot {

    fun verifyHomePageView(rule: ComposeTestRule) {
        assertMostVisitedTopSitesButton(rule)
        assertJumpBackInButton(rule)
        assertRecentBookmarksButton(rule)
        assertRecentSearchesButton(rule)
        assertOpeningScreenHeading(rule)
        assertHomepageButton(rule)
        assertLastTabButton(rule)
        assertHomepageAfterFourHoursButton(rule)
    }

    fun clickSponsoredShortcuts() = sponsoredShortcuts().click()

    fun clickJumpBackInButton(rule: ComposeTestRule) = jumpBackInButton(rule).performClick()

    fun clickRecentBookmarksButton(rule: ComposeTestRule) =
        recentBookmarksButton(rule).performClick()

    fun clickRecentSearchesButton(rule: ComposeTestRule) = recentSearchesButton(rule).performClick()

    fun clickStartOnHomepageButton(rule: ComposeTestRule) = homepageButton(rule).performClick()

    fun clickStartOnLastTabButton(rule: ComposeTestRule) = lastTabButton(rule).performClick()

    fun openWallpapersMenu() = wallpapersMenuButton.click()

    fun selectWallpaper(wallpaperName: String) =
        mDevice.findObject(UiSelector().description(wallpaperName)).click()

    fun verifySnackBarText(expectedText: String) =
        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .textContains(expectedText)
            ).waitForExists(waitingTimeShort)
        )

    fun verifySponsoredShortcutsCheckBox(checked: Boolean) {
        if (checked) {
            sponsoredShortcuts()
                .check(
                    matches(
                        hasSibling(
                            withChild(
                                allOf(
                                    withClassName(endsWith("CheckBox")),
                                    isChecked()
                                )
                            )
                        )
                    )
                )
        } else {
            sponsoredShortcuts()
                .check(
                    matches(
                        hasSibling(
                            withChild(
                                allOf(
                                    withClassName(endsWith("CheckBox")),
                                    isNotChecked()
                                )
                            )
                        )
                    )
                )
        }
    }

    class Transition {

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickSnackBarViewButton(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            val snackBarButton = mDevice.findObject(UiSelector().text("VIEW"))
            snackBarButton.waitForExists(waitingTimeShort)
            snackBarButton.click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

private fun mostVisitedTopSitesButton(rule: ComposeTestRule): SemanticsNodeInteraction =
    rule.onNodeWithText(getStringResource(R.string.top_sites_toggle_top_recent_sites_4))

private fun sponsoredShortcuts() =
    onView(allOf(withText(R.string.customize_toggle_contile)))

private fun jumpBackInButton(rule: ComposeTestRule): SemanticsNodeInteraction =
    rule.onNodeWithText(getStringResource(R.string.customize_toggle_jump_back_in))

private fun recentBookmarksButton(rule: ComposeTestRule): SemanticsNodeInteraction =
    rule.onNodeWithText(getStringResource(R.string.customize_toggle_recent_bookmarks))

private fun recentSearchesButton(rule: ComposeTestRule): SemanticsNodeInteraction =
    rule.onNodeWithText(getStringResource(R.string.customize_toggle_recently_visited))

private fun openingScreenHeading(rule: ComposeTestRule): SemanticsNodeInteraction =
    rule.onNodeWithText(getStringResource(R.string.preferences_opening_screen))

private fun homepageButton(rule: ComposeTestRule) = rule.onNode(
    hasTestTag("radio.button.preference") and
            hasText(getStringResource(R.string.opening_screen_homepage))
)

private fun lastTabButton(rule: ComposeTestRule) = rule.onNode(
    hasTestTag("radio.button.preference") and
            hasText(getStringResource(R.string.opening_screen_last_tab))
)

private fun homepageAfterFourHoursButton(rule: ComposeTestRule) = rule.onNode(
    hasTestTag("radio.button.preference") and
            hasText(getStringResource(R.string.opening_screen_after_four_hours_of_inactivity))
)

private fun goBackButton() = onView(allOf(withContentDescription(R.string.action_bar_up_description)))

private fun assertMostVisitedTopSitesButton(rule: ComposeTestRule) =
    mostVisitedTopSitesButton(rule).assertIsDisplayed()
private fun assertJumpBackInButton(rule: ComposeTestRule) =
    jumpBackInButton(rule).assertIsDisplayed()
private fun assertRecentBookmarksButton(rule: ComposeTestRule) =
    recentBookmarksButton(rule).assertIsDisplayed()
private fun assertRecentSearchesButton(rule: ComposeTestRule) =
    recentSearchesButton(rule).assertIsDisplayed()
private fun assertOpeningScreenHeading(rule: ComposeTestRule) =
    openingScreenHeading(rule).assertIsDisplayed()
private fun assertHomepageButton(rule: ComposeTestRule) =
    homepageButton(rule).assertIsDisplayed()
private fun assertLastTabButton(rule: ComposeTestRule) =
    lastTabButton(rule).assertIsDisplayed()
private fun assertHomepageAfterFourHoursButton(rule: ComposeTestRule) =
    homepageAfterFourHoursButton(rule).assertIsDisplayed()

private val wallpapersMenuButton = onView(withText("Wallpapers"))
