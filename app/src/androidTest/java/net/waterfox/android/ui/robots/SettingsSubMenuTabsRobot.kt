/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import net.waterfox.android.R
import net.waterfox.android.helpers.TestHelper.getStringResource
import net.waterfox.android.helpers.TestHelper.mDevice
import org.hamcrest.CoreMatchers.allOf

/**
 * Implementation of Robot Pattern for the settings Tabs sub menu.
 */
class SettingsSubMenuTabsRobot {

    fun verifyTabViewOptions(rule: ComposeTestRule) = assertTabViewOptions(rule)

    fun verifyCloseTabsOptions(rule: ComposeTestRule) = assertCloseTabsOptions(rule)

    fun verifyMoveOldTabsToInactiveOptions(rule: ComposeTestRule) =
        assertMoveOldTabsToInactiveOptions(rule)

    class Transition {
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun assertTabViewOptions(rule: ComposeTestRule) {
    tabViewHeading(rule).assertIsDisplayed()
    listToggle(rule).assertIsDisplayed()
    gridToggle(rule).assertIsDisplayed()
}

private fun assertCloseTabsOptions(rule: ComposeTestRule) {
    closeTabsHeading(rule).assertIsDisplayed()
    neverToggle(rule).assertIsDisplayed()
    afterOneDayToggle(rule).assertIsDisplayed()
    afterOneWeekToggle(rule).assertIsDisplayed()
    afterOneMonthToggle(rule).assertIsDisplayed()
}

private fun assertMoveOldTabsToInactiveOptions(rule: ComposeTestRule) {
    moveOldTabsToInactiveHeading(rule).assertDoesNotExist()
    moveOldTabsToInactiveToggle(rule).assertDoesNotExist()
}

private fun tabViewHeading(rule: ComposeTestRule) = rule.onNodeWithText("Tab view")

private fun listToggle(rule: ComposeTestRule) = rule.onNodeWithText("List")

private fun gridToggle(rule: ComposeTestRule) = rule.onNodeWithText("Grid")

private fun closeTabsHeading(rule: ComposeTestRule) = rule.onNodeWithText("Close tabs")

private fun neverToggle(rule: ComposeTestRule) = rule.onNodeWithText("Never")

private fun afterOneDayToggle(rule: ComposeTestRule) = rule.onNodeWithText("After one day")

private fun afterOneWeekToggle(rule: ComposeTestRule) = rule.onNodeWithText("After one week")

private fun afterOneMonthToggle(rule: ComposeTestRule) = rule.onNodeWithText("After one month")

private fun moveOldTabsToInactiveHeading(rule: ComposeTestRule) =
    rule.onNodeWithText("Move old tabs to inactive")

private fun moveOldTabsToInactiveToggle(rule: ComposeTestRule) =
    rule.onNodeWithText(getStringResource(R.string.preferences_inactive_tabs_title))

private fun goBackButton() =
    onView(allOf(ViewMatchers.withContentDescription("Navigate up")))
