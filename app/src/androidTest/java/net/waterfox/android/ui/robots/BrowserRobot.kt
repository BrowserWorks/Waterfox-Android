/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package net.waterfox.android.ui.robots

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.BundleMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import net.waterfox.android.R
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.Constants.LONG_CLICK_DURATION
import net.waterfox.android.helpers.Constants.RETRY_COUNT
import net.waterfox.android.helpers.MatcherHelper
import net.waterfox.android.helpers.SessionLoadedIdlingResource
import net.waterfox.android.helpers.TestAssetHelper.waitingTime
import net.waterfox.android.helpers.TestAssetHelper.waitingTimeLong
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.TestHelper.packageName
import net.waterfox.android.helpers.TestHelper.waitForObjects
import net.waterfox.android.helpers.click
import net.waterfox.android.helpers.ext.waitNotNull

class BrowserRobot {
    private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

    fun waitForPageToLoad() = progressBar.waitUntilGone(waitingTime)

    fun verifyCurrentPrivateSession(context: Context) {
        val selectedTab = context.components.core.store.state.selectedTab
        assertTrue("Current session is private", selectedTab?.content?.private ?: false)
    }

    fun verifyUrl(url: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .resourceId("$packageName:id/mozac_browser_toolbar_url_view")
                        .textContains(url.replace("http://", ""))
                ).waitForExists(waitingTime)
            )
        }
    }

    fun verifyHelpUrl() {
        verifyUrl("support.mozilla.org/")
    }

    fun verifyWhatsNewURL() {
        verifyUrl("support.mozilla.org/")
    }

    fun verifyRateOnGooglePlayURL() {
        verifyUrl("play.google.com/store/apps/details?id=net.waterfox.android")
    }

    /* Asserts that the text within DOM element with ID="testContent" has the given text, i.e.
    *  document.querySelector('#testContent').innerText == expectedText
    *
    */

    fun verifyPageContent(expectedText: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        mDevice.waitNotNull(
            Until.findObject(By.res("$packageName:id/engineView")),
            waitingTime
        )

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                "Page didn't load or doesn't contain the expected text",
                mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
            )
        }
    }

    /* Verifies the information displayed on the about:cache page */
    fun verifyNetworkCacheIsEmpty(storage: String) {
        val memorySection = mDevice.findObject(UiSelector().description(storage))

        val gridView =
            if (storage == "memory") {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(2)
                )
            } else {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(4)
                )
            }

        val cacheSizeInfo =
            gridView.getChild(
                UiSelector().text("Number of entries:")
            ).getFromParent(
                UiSelector().text("0")
            )

        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(cacheSizeInfo.waitForExists(waitingTime))
                break
            } catch (e: AssertionError) {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage { }
            }
        }
    }

    fun verifyTabCounter(expectedText: String) {
        val counter =
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/counter_text")
                    .text(expectedText)
            )
        assertTrue(counter.waitForExists(waitingTime))
    }

    fun verifySnackBarText(expectedText: String) {
        mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains(expectedText)))

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .textContains(expectedText)
            ).waitForExists(waitingTime)
        )
    }

    fun verifyLinkContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(
            Until.findObject(By.textContains(containsURL.toString())),
            waitingTime
        )
        mDevice.waitNotNull(
            Until.findObject(text("Open link in new tab")),
            waitingTime
        )
        mDevice.waitNotNull(
            Until.findObject(text("Open link in private tab")),
            waitingTime
        )
        mDevice.waitNotNull(Until.findObject(text("Copy link")), waitingTime)
        mDevice.waitNotNull(Until.findObject(text("Share link")), waitingTime)
    }

    fun verifyLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open link in new tab")), waitingTime
        )
        mDevice.waitNotNull(
            Until.findObject(text("Open link in private tab")), waitingTime
        )
        mDevice.waitNotNull(Until.findObject(text("Copy link")), waitingTime)
        mDevice.waitNotNull(Until.findObject(text("Share link")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")), waitingTime
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")), waitingTime
        )
    }

    fun verifyNavURLBarHidden() = assertNavURLBarHidden()

    fun verifySecureConnectionLockIcon() = assertSecureConnectionLockIcon()

    fun verifyMenuButton() = assertMenuButton()

    fun verifyNavURLBarItems() {
        navURLBar().waitForExists(waitingTime)
        verifyMenuButton()
        verifyTabCounter("1")
        verifySearchBar()
        verifySecureConnectionLockIcon()
        verifyHomeScreenButton()
    }

    fun verifyNoLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")), waitingTime
        )
    }

    fun verifyNotificationDotOnMainMenu() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/notification_dot"))
                .waitForExists(waitingTime)
        )
    }

    fun verifyHomeScreenButton() = assertHomeScreenButton()

    fun verifySearchBar() = assertSearchBar()

    fun dismissContentContextMenu(containsURL: Uri) {
        onView(withText(containsURL.toString()))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(ViewActions.pressBack())
    }

    fun clickContextOpenLinkInNewTab() {
        mDevice.waitNotNull(
            Until.findObject(text("Open link in new tab")),
            waitingTime
        )

        val menuOpenInNewTab = mDevice.findObject(text("Open link in new tab"))
        menuOpenInNewTab.click()
    }

    fun clickContextOpenLinkInPrivateTab() {
        mDevice.waitNotNull(
            Until.findObject(text("Open link in private tab")),
            waitingTime
        )

        val menuOpenInPrivateTab = mDevice.findObject(text("Open link in private tab"))
        menuOpenInPrivateTab.click()
    }

    fun clickContextCopyLink() {
        mDevice.waitNotNull(Until.findObject(text("Copy link")), waitingTime)

        val menuCopyLink = mDevice.findObject(text("Copy link"))
        menuCopyLink.click()
    }

    fun clickContextShareLink(url: Uri) {
        mDevice.waitNotNull(Until.findObject(text("Share link")), waitingTime)

        val menuShareLink = mDevice.findObject(text("Share link"))
        menuShareLink.click()

        // verify share intent is launched and matched with associated passed in URL
        Intents.intended(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_CHOOSER),
                IntentMatchers.hasExtras(
                    allOf(
                        BundleMatchers.hasEntry(
                            Intent.EXTRA_INTENT,
                            allOf(
                                IntentMatchers.hasAction(Intent.ACTION_SEND),
                                IntentMatchers.hasType("text/plain"),
                                IntentMatchers.hasExtra(
                                    Intent.EXTRA_TEXT,
                                    url.toString()
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    fun clickContextCopyImageLocation() {
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")),
            waitingTime
        )

        val menuCopyImageLocation = mDevice.findObject(text("Copy image location"))
        menuCopyImageLocation.click()
    }

    fun clickContextOpenImageNewTab() {
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime
        )

        val menuOpenImageNewTab = mDevice.findObject(text("Open image in new tab"))
        menuOpenImageNewTab.click()
    }

    fun clickContextSaveImage() {
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)

        val menuSaveImage = mDevice.findObject(text("Save image"))
        menuSaveImage.click()
    }

    fun createBookmark(url: Uri) {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(url) {
            // needs to wait for the right url to load before saving a bookmark
            verifyUrl(url.toString())
        }.openThreeDotMenu {
        }.bookmarkPage { }
    }

    fun clickLinkMatchingText(expectedText: String) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTimeLong)

        val element = mDevice.findObject(UiSelector().textContains(expectedText))
        element.click()
    }

    fun longClickMatchingText(expectedText: String) {
        try {
            mDevice.waitForWindowUpdate(packageName, waitingTime)
            mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
                .waitForExists(waitingTime)
            mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
            val link = mDevice.findObject(By.textContains(expectedText))
            link.click(LONG_CLICK_DURATION)
        } catch (e: NullPointerException) {
            println(e)

            // Refresh the page in case the first long click didn't succeed
            navigationToolbar {
            }.openThreeDotMenu {
            }.refreshPage {
                mDevice.waitForIdle()
            }

            // Long click again the desired text
            mDevice.waitForWindowUpdate(packageName, waitingTime)
            mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
                .waitForExists(waitingTime)
            mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
            val link = mDevice.findObject(By.textContains(expectedText))
            link.click(LONG_CLICK_DURATION)
        }
    }

    fun longClickAndCopyText(expectedText: String, selectAll: Boolean = false) {
        try {
            // Long click desired text
            mDevice.waitForWindowUpdate(packageName, waitingTime)
            mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
                .waitForExists(waitingTime)
            mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
            val link = mDevice.findObject(By.textContains(expectedText))
            link.click(LONG_CLICK_DURATION)

            // Click Select all from the text selection toolbar
            if (selectAll) {
                mDevice.findObject(UiSelector().textContains("Select all")).waitForExists(waitingTime)
                val selectAllText = mDevice.findObject(By.textContains("Select all"))
                selectAllText.click()
            }

            // Click Copy from the text selection toolbar
            mDevice.findObject(UiSelector().textContains("Copy")).waitForExists(waitingTime)
            val copyText = mDevice.findObject(By.textContains("Copy"))
            copyText.click()
        } catch (e: NullPointerException) {
            println("Failed to long click desired text: ${e.localizedMessage}")

            // Refresh the page in case the first long click didn't succeed
            navigationToolbar {
            }.openThreeDotMenu {
            }.refreshPage {
                mDevice.waitForIdle()
            }

            // Long click again the desired text
            mDevice.waitForWindowUpdate(packageName, waitingTime)
            mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
                .waitForExists(waitingTime)
            mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
            val link = mDevice.findObject(By.textContains(expectedText))
            link.click(LONG_CLICK_DURATION)

            // Click again Select all from the text selection toolbar
            if (selectAll) {
                mDevice.findObject(UiSelector().textContains("Select all")).waitForExists(waitingTime)
                val selectAllText = mDevice.findObject(By.textContains("Select all"))
                selectAllText.click()
            }

            // Click again Copy from the text selection toolbar
            mDevice.findObject(UiSelector().textContains("Copy")).waitForExists(waitingTime)
            val copyText = mDevice.findObject(By.textContains("Copy"))
            copyText.click()
        }
    }

    fun longClickAndSearchText(searchButton: String, expectedText: String) {
        var currentTries = 0
        while (currentTries++ < 3) {
            try {
                // Long click desired text
                mDevice.waitForWindowUpdate(packageName, waitingTime)
                mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
                    .waitForExists(waitingTime)
                mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)
                val link = mDevice.findObject(By.textContains(expectedText))
                link.click(LONG_CLICK_DURATION)

                // Click search from the text selection toolbar
                mDevice.findObject(UiSelector().textContains(searchButton)).waitForExists(waitingTime)
                val searchText = mDevice.findObject(By.textContains(searchButton))
                searchText.click()

                break
            } catch (e: NullPointerException) {
                println("Failed to long click desired text: ${e.localizedMessage}")

                // Refresh the page in case the first long click didn't succeed
                navigationToolbar {
                }.openThreeDotMenu {
                }.refreshPage {
                    mDevice.waitForIdle()
                }
            }
        }
    }

    fun snackBarButtonClick() {
        val switchButton =
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/snackbar_btn")
            )
        switchButton.waitForExists(waitingTime)
        switchButton.clickAndWaitForNewWindow(waitingTime)
    }

    fun verifySaveLoginPromptIsShown() {
        mDevice.findObject(UiSelector().text("test@example.com")).waitForExists(waitingTime)
        val submitButton = mDevice.findObject(By.res("submit"))
        submitButton.clickAndWait(Until.newWindow(), waitingTime)
        // Click save to save the login
        mDevice.waitNotNull(Until.findObjects(text("Save")))
    }

    fun verifyUpdateLoginPromptIsShown() {
        val submitButton = mDevice.findObject(By.res("submit"))
        submitButton.clickAndWait(Until.newWindow(), waitingTime)

        mDevice.waitNotNull(Until.findObjects(text("Update")))
    }

    fun saveLoginFromPrompt(optionToSaveLogin: String) {
        mDevice.waitForObjects(
            mDevice.findObject(
                UiSelector().resourceId("$packageName:id/feature_prompt_login_fragment")
            )
        )
        mDevice.findObject(text(optionToSaveLogin)).click()
    }

    fun enterPassword(password: String) {
        val passwordField = mDevice.findObject(
            UiSelector()
                .resourceId("password")
                .className(EditText::class.java)
        )
        try {
            passwordField.waitForExists(waitingTime)
            mDevice.findObject(
                By
                    .res("password")
                    .clazz(EditText::class.java)
            ).click()
            passwordField.clearTextField()
            passwordField.text = password
            // wait until the password is hidden
            assertTrue(mDevice.findObject(UiSelector().text(password)).waitUntilGone(waitingTime))
        } catch (e: UiObjectNotFoundException) {
            println(e)

            // Lets refresh the page and try again
            browserScreen {
            }.openThreeDotMenu {
            }.refreshPage {
                mDevice.waitForIdle()
            }
        } finally {
            passwordField.waitForExists(waitingTime)
            mDevice.findObject(
                By
                    .res("password")
                    .clazz(EditText::class.java)
            ).click()
            passwordField.clearTextField()
            passwordField.text = password
            // wait until the password is hidden
            assertTrue(mDevice.findObject(UiSelector().text(password)).waitUntilGone(waitingTime))
        }
    }

    fun clickMediaPlayerPlayButton() {
        mediaPlayerPlayButton().waitForExists(waitingTime)
        mediaPlayerPlayButton().click()
    }

    /**
     * Get the current playback state of the currently selected tab.
     * The result may be null if there if the currently playing media tab cannot be found in [store]
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @return nullable [MediaSession.PlaybackState] indicating the media playback state for the current tab.
     */
    private fun getCurrentPlaybackState(store: BrowserStore): MediaSession.PlaybackState? {
        return store.state.selectedTab?.mediaSessionState?.playbackState
    }

    /**
     * Asserts that in [waitingTime] the playback state of the current tab will be [expectedState].
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @param expectedState [MediaSession.PlaybackState] the playback state that will be asserted
     * @param waitingTime maximum time the test will wait for the playback state to become [expectedState]
     * before failing the assertion.
     */
    fun assertPlaybackState(store: BrowserStore, expectedState: MediaSession.PlaybackState) {
        val startMills = SystemClock.uptimeMillis()
        var currentMills: Long = 0
        while (currentMills <= waitingTime) {
            if (expectedState == getCurrentPlaybackState(store)) return
            currentMills = SystemClock.uptimeMillis() - startMills
        }
        fail("Playback did not moved to state: $expectedState")
    }

    fun swipeNavBarRight(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            navURLBar().swipeRight(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        } catch (e: AssertionError) {
            navURLBar().swipeRight(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        }
    }

    fun swipeNavBarLeft(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            navURLBar().swipeLeft(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        } catch (e: AssertionError) {
            navURLBar().swipeLeft(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        }
    }

    fun clickTabCrashedRestoreButton() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/restoreTabButton"))
                .waitForExists(waitingTime)
        )

        val tabCrashRestoreButton = mDevice.findObject(UiSelector().resourceIdMatches("$packageName:id/restoreTabButton"))
        tabCrashRestoreButton.click()
    }

    fun fillAndSubmitLoginCredentials(userName: String, password: String) {
        var currentTries = 0
        while (currentTries++ < 3) {
            try {
                mDevice.waitForIdle(waitingTime)
                userNameTextBox.setText(userName)
                passwordTextBox.setText(password)
                submitLoginButton.click()

                mDevice.waitForObjects(mDevice.findObject(UiSelector().resourceId("$packageName:id/save_confirm")))

                break
            } catch (e: UiObjectNotFoundException) {
                Log.e("BROWSER_ROBOT", "Failed to find locator: ${e.localizedMessage}")
            }
        }
    }

    fun clearUserNameLoginCredential() {
        mDevice.waitForObjects(userNameTextBox)
        userNameTextBox.clearTextField()
        mDevice.waitForIdle(waitingTime)
    }

    fun clickSuggestedLoginsButton() {
        var currentTries = 0
        while (currentTries++ < 3) {
            try {
                mDevice.waitForObjects(suggestedLogins)
                suggestedLogins.click()
                mDevice.waitForObjects(suggestedLogins)
                break
            } catch (e: UiObjectNotFoundException) {
                userNameTextBox.click()
            }
        }
    }

    fun clickStreetAddressTextBox() {
        streetAddressTextBox().waitForExists(waitingTime)
        streetAddressTextBox().click()
    }

    fun clickSelectAddressButton() {
        selectAddressButton.waitForExists(waitingTime)
        selectAddressButton.clickAndWaitForNewWindow(waitingTime)
    }

    fun clickCardNumberTextBox() {
        creditCardNumberTextBox().waitForExists(waitingTime)
        creditCardNumberTextBox().click()
    }

    fun clickSelectCreditCardButton() {
        selectCreditCardButton.waitForExists(waitingTime)
        selectCreditCardButton.clickAndWaitForNewWindow(waitingTime)
    }

    fun clickLoginSuggestion(userName: String) {
        val loginSuggestion =
            mDevice.findObject(
                UiSelector()
                    .textContains(userName)
                    .resourceId("$packageName:id/username")
            )

        loginSuggestion.click()
    }

    fun clickAddressSuggestion(streetName: String) {
        addressSuggestion(streetName).waitForExists(waitingTime)
        addressSuggestion(streetName).click()
    }

    fun clickCreditCardSuggestion(creditCardNumber: String) {
        creditCardSuggestion(creditCardNumber).waitForExists(waitingTime)
        creditCardSuggestion(creditCardNumber).click()
    }

    fun verifySuggestedUserName(userName: String) {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_feature_login_multiselect_expand")
        ).waitForExists(waitingTime)

        assertTrue(
            mDevice.findObject(UiSelector().textContains(userName)).waitForExists(waitingTime)
        )
    }

    fun verifyPrefilledLoginCredentials(userName: String) {
        var currentTries = 0
        // Sometimes the assertion of the pre-filled logins fails so we are re-trying after refreshing the page
        while (currentTries++ < 3) {
            try {
                mDevice.waitForObjects(userNameTextBox)
                assertTrue(userNameTextBox.text.equals(userName))

                break
            } catch (e: AssertionError) {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    clearUserNameLoginCredential()
                    clickSuggestedLoginsButton()
                    verifySuggestedUserName(userName)
                    clickLoginSuggestion(userName)
                }
            }
        }
        mDevice.waitForObjects(userNameTextBox)
        assertTrue(userNameTextBox.text.equals(userName))
    }

    fun verifyAutofilledAddress(streetAddress: String) {
        mDevice.waitForObjects(streetAddressTextBox(streetAddress))
        assertTrue(streetAddressTextBox(streetAddress).waitForExists(waitingTime))
    }

    fun verifyAutofilledCreditCard(creditCardNumber: String) {
        mDevice.waitForObjects(creditCardNumberTextBox(creditCardNumber))
        assertTrue(creditCardNumberTextBox(creditCardNumber).waitForExists(waitingTime))
    }

    fun verifyPrefilledPWALoginCredentials(userName: String, shortcutTitle: String) {
        mDevice.waitForIdle(waitingTime)

        var currentTries = 0
        while (currentTries++ < 3) {
            try {
                assertTrue(submitLoginButton.waitForExists(waitingTime))
                submitLoginButton.click()
                assertTrue(userNameTextBox.text.equals(userName))
                break
            } catch (e: AssertionError) {
                addToHomeScreen {
                }.searchAndOpenHomeScreenShortcut(shortcutTitle) {}
            }
        }
    }

    fun verifySaveLoginPromptIsDisplayed() {
        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/feature_prompt_login_fragment")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyTrackingProtectionWebContent(state: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector().textContains(state)
                    ).waitForExists(waitingTimeLong)
                )

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    Log.e("TestLog", "On try $i, trackers are not: $state")

                    navigationToolbar {
                    }.openThreeDotMenu {
                    }.refreshPage {
                    }
                }
            }
        }
    }

    fun clickSetCookiesButton() {
        val setCookiesButton = mDevice.findObject(UiSelector().resourceId("setCookies"))
        setCookiesButton.waitForExists(waitingTimeLong)
        setCookiesButton.click()
    }

    class Transition {
        private fun threeDotButton() = onView(
            allOf(
                ViewMatchers.withContentDescription(
                    "Menu"
                )
            )
        )

        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            mDevice.waitForIdle(waitingTime)
            threeDotButton().perform(ViewActions.click())

            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openNavigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
                .waitForExists(waitingTime)
            navURLBar().click()

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openTabDrawer(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                try {
                    mDevice.waitForObjects(
                        mDevice.findObject(
                            UiSelector()
                                .resourceId("$packageName:id/mozac_browser_toolbar_browser_actions"),
                        ),
                        waitingTime,
                    )

                    tabsCounter().click()
                    assertTrue(
                        MatcherHelper.itemWithResId("$packageName:id/new_tab_button")
                            .waitForExists(waitingTime),
                    )

                    break
                } catch (e: AssertionError) {
                    if (i == RETRY_COUNT) {
                        throw e
                    } else {
                        mDevice.waitForIdle()
                    }
                }
            }
            assertTrue(MatcherHelper.itemWithResId("$packageName:id/new_tab_button").waitForExists(waitingTime))

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun openTabButtonShortcutsMenu(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.desc("Tabs")))
            tabsCounter().click(LONG_CLICK_DURATION)

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openNotificationShade(interact: NotificationRobot.() -> Unit): NotificationRobot.Transition {
            mDevice.openNotification()

            NotificationRobot().interact()
            return NotificationRobot.Transition()
        }

        fun goToHomescreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            assertTrue(
                mDevice.findObject(UiSelector().description("Home screen"))
                    .waitForExists(waitingTime)
            )

            onView(withContentDescription("Home screen"))
                .check(matches(isDisplayed()))
                .click()

            assertTrue(
                mDevice.findObject(UiSelector().resourceId("$packageName:id/homeLayout"))
                    .waitForExists(waitingTime)
            )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.pressBack()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickTabCrashedCloseButton(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            assertTrue(
                mDevice.findObject(UiSelector().resourceId("$packageName:id/closeTabButton"))
                    .waitForExists(waitingTime)
            )

            val tabCrashedCloseButton = mDevice.findObject(text("Close tab"))
            tabCrashedCloseButton.click()

            mDevice.waitForIdle()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickShareSelectedText(interact: ShareOverlayRobot.() -> Unit): ShareOverlayRobot.Transition {
            val shareTextButton = mDevice.findObject(By.textContains("Share"))
            shareTextButton.click()

            ShareOverlayRobot().interact()
            return ShareOverlayRobot.Transition()
        }

        fun clickDownloadLink(title: String, interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            val downloadLink = mDevice.findObject(UiSelector().textContains(title))

            assertTrue(
                "$title download link not found",
                downloadLink.waitForExists(waitingTime)
            )
            downloadLink.click()

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }

        fun clickStartCameraButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            cameraButton.waitForExists(waitingTime)
            cameraButton.click()

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartMicrophoneButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            microphoneButton.waitForExists(waitingTime)
            microphoneButton.click()

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartAudioVideoButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            audioVideoButton.waitForExists(waitingTime)
            audioVideoButton.click()

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickOpenNotificationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            notificationButton.waitForExists(waitingTime)
            notificationButton.click()
            mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains("Allow to send notifications?")))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickGetLocationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            getLocationButton.waitForExists(waitingTime)
            getLocationButton.click()

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }
    }
}

fun browserScreen(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
    BrowserRobot().interact()
    return BrowserRobot.Transition()
}

fun navURLBar() = mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))

fun searchBar() = onView(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_url_view))

fun homeScreenButton() = onView(withContentDescription(R.string.browser_toolbar_home))

private fun assertHomeScreenButton() =
    homeScreenButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchBar() = searchBar().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertNavURLBar() = assertTrue(navURLBar().waitForExists(waitingTime))

private fun assertNavURLBarHidden() = assertTrue(navURLBar().waitUntilGone(waitingTime))

private fun assertSecureConnectionLockIcon() {
    onView(withId(mozilla.components.browser.toolbar.R.id.mozac_browser_toolbar_site_info_indicator))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun menuButton() = onView(withId(R.id.icon))

private fun assertMenuButton() {
    menuButton()
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun tabsCounter() = mDevice.findObject(By.res("$packageName:id/counter_root"))

private fun mediaPlayerPlayButton() =
    mDevice.findObject(
        UiSelector()
            .className("android.widget.Button")
            .text("Play")
    )

private var progressBar =
    mDevice.findObject(
        UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_progress")
    )

private val suggestedLogins = mDevice.findObject(UiSelector().resourceId("$packageName:id/loginSelectBar"))
private val selectAddressButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/select_address_header"))
private val selectCreditCardButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/select_credit_card_header"))

private fun addressSuggestion(streetName: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/address_name")
            .textContains(streetName)
    )

private fun streetAddressTextBox(streetAddress: String = "") =
    mDevice.findObject(
        UiSelector()
            .resourceId("streetAddress")
            .textContains(streetAddress)
            .className("android.widget.EditText")
            .packageName("$packageName")
    )

private fun creditCardNumberTextBox(creditCardNumber: String = "") =
    mDevice.findObject(
        UiSelector()
            .resourceId("cardNumber")
            .textContains(creditCardNumber)
            .className("android.widget.EditText")
            .packageName("$packageName")
    )

private fun creditCardSuggestion(creditCardNumber: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/credit_card_number")
            .textContains(creditCardNumber)
    )

// Permissions test page elements & prompts
// Test page used located at https://mozilla-mobile.github.io/testapp/permissions
private val cameraButton = mDevice.findObject(UiSelector().text("Open camera"))

private val microphoneButton = mDevice.findObject(UiSelector().text("Open microphone"))

private val audioVideoButton = mDevice.findObject(UiSelector().text("Camera & Microphone"))

private val notificationButton = mDevice.findObject(UiSelector().text("Open notifications dialogue"))

private val getLocationButton = mDevice.findObject(UiSelector().text("Get Location"))

// Login form test page elements
// Test page used located at https://mozilla-mobile.github.io/testapp/loginForm
val userNameTextBox =
    mDevice.findObject(
        UiSelector()
            .resourceId("username")
            .className("android.widget.EditText")
            .packageName("$packageName")
    )

private val submitLoginButton =
    mDevice.findObject(
        UiSelector()
            .resourceId("submit")
            .textContains("Submit Query")
            .className("android.widget.Button")
            .packageName("$packageName")
    )

val passwordTextBox =
    mDevice.findObject(
        UiSelector()
            .resourceId("password")
            .className("android.widget.EditText")
            .packageName("$packageName")
    )
