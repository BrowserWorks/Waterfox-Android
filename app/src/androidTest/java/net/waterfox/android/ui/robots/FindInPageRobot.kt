/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import net.waterfox.android.R
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the find in page UI.
 */
class FindInPageRobot {
    fun verifyFindInPageQuery() = assertFindInPageQuery()!!
    fun verifyFindInPageNextButton() = assertFindInPageNextButton()!!
    fun verifyFindInPagePrevButton() = assertFindInPagePrevButton()!!
    fun verifyFindInPageCloseButton() = assertFindInPageCloseButton()!!
    fun clickFindInPageNextButton() = findInPageNextButton().click()
    fun clickFindInPagePrevButton() = findInPagePrevButton().click()

    fun verifyFindInPageSearchBarItems() {
        verifyFindInPageQuery()
        verifyFindInPageNextButton()
        verifyFindInPagePrevButton()
        verifyFindInPageCloseButton()
    }

    fun enterFindInPageQuery(expectedText: String) {
        mDevice.waitNotNull(Until.findObject(By.res("net.waterfox.android.debug:id/find_in_page_query_text")), waitingTime)
        findInPageQuery().perform(clearText())
        mDevice.waitNotNull(Until.gone(By.res("net.waterfox.android.debug:id/find_in_page_result_text")), waitingTime)
        findInPageQuery().perform(typeText(expectedText))
        mDevice.waitNotNull(Until.findObject(By.res("net.waterfox.android.debug:id/find_in_page_result_text")), waitingTime)
    }

    fun verifyFindNextInPageResult(ratioCounter: String) {
        mDevice.waitNotNull(Until.findObject(By.text(ratioCounter)), waitingTime)
        findInPageResult().check(matches(withText((ratioCounter))))
    }

    fun verifyFindPrevInPageResult(ratioCounter: String) {
        mDevice.waitNotNull(Until.findObject(By.text(ratioCounter)), waitingTime)
        findInPageResult().check(matches(withText((ratioCounter))))
    }

    class Transition {
        fun closeFindInPage(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            findInPageCloseButton().click()
            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun findInPageQuery() = onView(withId(mozilla.components.feature.findinpage.R.id.find_in_page_query_text))
private fun findInPageResult() = onView(withId(mozilla.components.feature.findinpage.R.id.find_in_page_result_text))
private fun findInPageNextButton() = onView(withId(mozilla.components.feature.findinpage.R.id.find_in_page_next_btn))
private fun findInPagePrevButton() = onView(withId(mozilla.components.feature.findinpage.R.id.find_in_page_prev_btn))
private fun findInPageCloseButton() = onView(withId(mozilla.components.feature.findinpage.R.id.find_in_page_close_btn))

private fun assertFindInPageQuery() = findInPageQuery()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertFindInPageNextButton() = findInPageNextButton()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertFindInPagePrevButton() = findInPagePrevButton()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertFindInPageCloseButton() = findInPageCloseButton()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
