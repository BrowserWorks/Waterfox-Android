/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.net.Uri
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertTrue
import net.waterfox.android.R
import net.waterfox.android.helpers.SessionLoadedIdlingResource
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the URL toolbar.
 */
class NavigationToolbarRobot {

    fun verifyNoHistoryBookmarks() = assertNoHistoryBookmarks()

    fun verifyTabButtonShortcutMenuItems() = assertTabButtonShortcutMenuItems()

    fun verifyReaderViewDetected(visible: Boolean = false) =
        assertReaderViewDetected(visible)

    fun verifyCloseReaderViewDetected(visible: Boolean = false) =
        assertCloseReaderViewDetected(visible)

    fun typeSearchTerm(searchTerm: String) = awesomeBar().setText(searchTerm)

    fun toggleReaderView() {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_page_actions")
        )
            .waitForExists(waitingTime)

        readerViewToggle().click()
    }

    class Transition {
        private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

        fun goBackToWebsite(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            openEditURLView()
            clearAddressBar().click()
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
                        .textContains("")
                ).waitForExists(waitingTime)
            )

            goBackButton()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun enterURLAndEnterToBrowser(
            url: Uri,
            interact: BrowserRobot.() -> Unit
        ): BrowserRobot.Transition {
            sessionLoadedIdlingResource = SessionLoadedIdlingResource()

            openEditURLView()

            awesomeBar().setText(url.toString())
            mDevice.pressEnter()

            runWithIdleRes(sessionLoadedIdlingResource) {
                onView(
                    anyOf(
                        withResourceName("browserLayout"),
                        withResourceName("download_button")
                    )
                ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openTabCrashReporter(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            val crashUrl = "about:crashcontent"

            sessionLoadedIdlingResource = SessionLoadedIdlingResource()

            openEditURLView()

            awesomeBar().setText(crashUrl)
            mDevice.pressEnter()

            runWithIdleRes(sessionLoadedIdlingResource) {
                mDevice.findObject(UiSelector().resourceId("$packageName:id/crash_tab_image"))
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.res("$packageName:id/mozac_browser_toolbar_menu")), waitingTime)
            threeDotButton().click()

            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openTabTray(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            mDevice.waitForIdle(waitingTime)
            tabTrayButton().click()
            mDevice.waitNotNull(
                Until.findObject(By.res("$packageName:id/tab_layout")),
                waitingTime
            )

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun visitLinkFromClipboard(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(
                Until.findObject(By.res("net.waterfox.android.debug:id/mozac_browser_toolbar_clear_view")),
                waitingTime
            )
            clearAddressBar().click()

            mDevice.waitNotNull(
                Until.findObject(By.res("net.waterfox.android.debug:id/clipboard_title")),
                waitingTime
            )

            // On Android 12 or above we don't SHOW the URL unless the user requests to do so.
            // See for mor information https://github.com/mozilla-mobile/fenix/issues/22271
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                mDevice.waitNotNull(
                    Until.findObject(By.res("net.waterfox.android.debug:id/clipboard_url")),
                    waitingTime
                )
            }

            fillLinkButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun closeTabFromShortcutsMenu(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            mDevice.waitForIdle(waitingTime)

            onView(withId(mozilla.components.browser.menu2.R.id.mozac_browser_menu_recyclerView))
                .perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(
                            withText("Close tab")
                        ),
                        ViewActions.click()
                    )
                )

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openTabFromShortcutsMenu(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle(waitingTime)

            onView(withId(mozilla.components.browser.menu2.R.id.mozac_browser_menu_recyclerView))
                .perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(
                            withText("New tab")
                        ),
                        ViewActions.click()
                    )
                )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openNewPrivateTabFromShortcutsMenu(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle(waitingTime)

            onView(withId(mozilla.components.browser.menu2.R.id.mozac_browser_menu_recyclerView))
                .perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(
                            withText("New private tab")
                        ),
                        ViewActions.click()
                    )
                )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickUrlbar(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            urlBar().click()

            mDevice.findObject(
                UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
            ).waitForExists(waitingTime)

            SearchRobot().interact()
            return SearchRobot.Transition()
        }
    }
}

fun navigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
    NavigationToolbarRobot().interact()
    return NavigationToolbarRobot.Transition()
}

fun openEditURLView() {
    mDevice.waitNotNull(
        Until.findObject(By.res("$packageName:id/toolbar")),
        waitingTime
    )
    urlBar().click()
    mDevice.waitNotNull(
        Until.findObject(By.res("$packageName:id/mozac_browser_toolbar_edit_url_view")),
        waitingTime
    )
}

private fun assertNoHistoryBookmarks() {
    onView(withId(R.id.container))
        .check(matches(not(hasDescendant(withText("Test_Page_1")))))
        .check(matches(not(hasDescendant(withText("Test_Page_2")))))
        .check(matches(not(hasDescendant(withText("Test_Page_3")))))
}

private fun assertTabButtonShortcutMenuItems() {
    onView(withId(mozilla.components.browser.menu2.R.id.mozac_browser_menu_recyclerView))
        .check(matches(hasDescendant(withText("Close tab"))))
        .check(matches(hasDescendant(withText("New private tab"))))
        .check(matches(hasDescendant(withText("New tab"))))
}

private fun urlBar() = mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
private fun awesomeBar() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))
private fun threeDotButton() = onView(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_menu))
private fun tabTrayButton() = onView(withId(R.id.tab_button))
private fun fillLinkButton() = onView(withId(R.id.fill_link_from_clipboard))
private fun clearAddressBar() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_clear_view"))
private fun goBackButton() = mDevice.pressBack()
private fun readerViewToggle() =
    onView(withParent(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions)))

private fun assertReaderViewDetected(visible: Boolean) {
    mDevice.findObject(
        UiSelector()
            .description("Reader view")
    )
        .waitForExists(waitingTime)

    onView(
        allOf(
            withParent(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions)),
            withContentDescription("Reader view")
        )
    ).check(
        if (visible) matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
        else ViewAssertions.doesNotExist()
    )
}

private fun assertCloseReaderViewDetected(visible: Boolean) {
    mDevice.findObject(
        UiSelector()
            .description("Close reader view")
    )
        .waitForExists(waitingTime)

    onView(
        allOf(
            withParent(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_page_actions)),
            withContentDescription("Close reader view")
        )
    ).check(
        if (visible) matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
        else ViewAssertions.doesNotExist()
    )
}

inline fun runWithIdleRes(ir: IdlingResource?, pendingCheck: () -> Unit) {
    try {
        IdlingRegistry.getInstance().register(ir)
        pendingCheck()
    } finally {
        IdlingRegistry.getInstance().unregister(ir)
    }
}
