/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.utils

import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner

@RunWith(WaterfoxRobolectricTestRunner::class)
class ToolbarPopupWindowTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @Test
    fun `getUrlForClipboard should get the right URL`() {
        // Custom tab
        val customTabSession = createCustomTab("https://mozilla.org")
        var store = BrowserStore(BrowserState(customTabs = listOf(customTabSession)))
        assertEquals(
            "https://mozilla.org",
            ToolbarPopupWindow.getUrlForClipboard(store, customTabSession.id)
        )

        // Regular tab
        val regularTab = createTab(url = "http://waterfox.net")
        store = BrowserStore(BrowserState(tabs = listOf(regularTab), selectedTabId = regularTab.id))
        assertEquals("http://waterfox.net", ToolbarPopupWindow.getUrlForClipboard(store))

        // Reader Tab
        val readerTab = createTab(
            url = "moz-extension://1234",
            readerState = ReaderState(active = true, activeUrl = "https://blog.mozilla.org/123")
        )
        store = BrowserStore(BrowserState(tabs = listOf(readerTab), selectedTabId = readerTab.id))
        assertEquals("https://blog.mozilla.org/123", ToolbarPopupWindow.getUrlForClipboard(store))
    }

    @Test
    fun `getUrlForClipboard should get the updated URL`() {
        // Custom tab
        val customTabSession = createCustomTab("https://mozilla.org")
        var store = BrowserStore(BrowserState(customTabs = listOf(customTabSession)))
        store.dispatch(ContentAction.UpdateUrlAction(customTabSession.id, "https://waterfox.net")).joinBlocking()
        assertEquals(
            "https://waterfox.net",
            ToolbarPopupWindow.getUrlForClipboard(store, customTabSession.id)
        )

        // Regular tab
        val regularTab = createTab(url = "http://waterfox.net")
        store = BrowserStore(BrowserState(tabs = listOf(regularTab), selectedTabId = regularTab.id))
        store.dispatch(ContentAction.UpdateUrlAction(regularTab.id, "https://mozilla.org")).joinBlocking()
        assertEquals("https://mozilla.org", ToolbarPopupWindow.getUrlForClipboard(store))
    }
}
