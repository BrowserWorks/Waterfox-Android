/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import io.mockk.*
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.components.AppStore
import net.waterfox.android.ext.maxActiveTime
import net.waterfox.android.ext.potentialInactiveTabs
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.utils.Settings
import org.junit.Assert.assertTrue

@RunWith(WaterfoxRobolectricTestRunner::class)
class DefaultInactiveTabsControllerTest {

    private val appStore: AppStore = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val browserStore: BrowserStore = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is dismissed THEN update settings`() {
        val controller = spyk(createController())

        controller.dismissAutoCloseDialog()

        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is accepted THEN update settings`() {
        val controller = spyk(createController())

        controller.enableInactiveTabsAutoClose()

        verify { settings.closeTabsAfterOneMonth = true }
        verify { settings.closeTabsAfterOneWeek = false }
        verify { settings.closeTabsAfterOneDay = false }
        verify { settings.manuallyCloseTabs = false }
        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

//    @Test
//    fun `WHEN all inactive tabs are closed THEN perform the deletion and show a Snackbar`() {
//        var showSnackbarInvoked = false
//        val controller = createController(
//            showUndoSnackbar = {
//                showSnackbarInvoked = true
//            }
//        )
//        val inactiveTab: TabSessionState = mockk {
//            every { lastAccess } returns maxActiveTime
//            every { createdAt } returns 0
//            every { id } returns "24"
//            every { content } returns mockk {
//                every { private } returns false
//            }
//        }
//
//        try {
//            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
//            every { browserStore.state } returns mockk()
//            every { browserStore.state.potentialInactiveTabs } returns listOf(inactiveTab)
//
//            controller.deleteAllInactiveTabs()
//
//            verify { tabsUseCases.removeTabs(listOf("24")) }
//            assertTrue(showSnackbarInvoked)
//        } finally {
//            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
//        }
//    }

    private fun createController(
        showUndoSnackbar: (Boolean) -> Unit = { _ -> },
    ): DefaultInactiveTabsController {
        return DefaultInactiveTabsController(
            appStore = appStore,
            settings = settings,
            browserStore = browserStore,
            tabsUseCases = tabsUseCases,
            showUndoSnackbar = showUndoSnackbar,
        )
    }
}
