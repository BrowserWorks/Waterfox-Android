/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package net.waterfox.android.settings.deletebrowsingdata

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.downloads.DownloadsUseCases.RemoveAllDownloadsUseCase
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import net.waterfox.android.HomeActivity
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.components.PermissionStorage
import net.waterfox.android.ext.components
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.CACHE
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.COOKIES
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.DOWNLOADS
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.HISTORY
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.PERMISSIONS
import net.waterfox.android.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType.TABS
import net.waterfox.android.utils.Settings

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAndQuitTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    private val activity: HomeActivity = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val tabUseCases: TabsUseCases = mockk(relaxed = true)
    private val historyStorage: PlacesHistoryStorage = mockk(relaxed = true)
    private val permissionStorage: PermissionStorage = mockk(relaxed = true)
    private val iconsStorage: BrowserIcons = mockk()
    private val engine: Engine = mockk(relaxed = true)
    private val removeAllTabsUseCases: TabsUseCases.RemoveAllTabsUseCase = mockk(relaxed = true)
    private val snackbar = mockk<WaterfoxSnackbar>(relaxed = true)
    private val downloadsUseCases: RemoveAllDownloadsUseCase = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { activity.components.core.historyStorage } returns historyStorage
        every { activity.components.core.permissionStorage } returns permissionStorage
        every { activity.components.useCases.tabsUseCases } returns tabUseCases
        every { activity.components.useCases.downloadUseCases.removeAllDownloads } returns downloadsUseCases
        every { tabUseCases.removeAllTabs } returns removeAllTabsUseCases
        every { activity.components.core.engine } returns engine
        every { activity.components.settings } returns settings
        every { activity.components.core.icons } returns iconsStorage
    }

    @Ignore("Failing test; need more investigation.")
    @Test
    fun `delete only tabs and quit`() = runTestOnMain {
        // When
        every { settings.getDeleteDataOnQuit(TABS) } returns true

        deleteAndQuit(activity, this, snackbar)

        advanceUntilIdle()

        verifyOrder {
            snackbar.show()
            removeAllTabsUseCases.invoke(false)
            activity.finishAndRemoveTask()
        }

        coVerify(exactly = 0) {
            engine.clearData(
                Engine.BrowsingData.select(
                    Engine.BrowsingData.COOKIES
                )
            )

            permissionStorage.deleteAllSitePermissions()

            engine.clearData(Engine.BrowsingData.allCaches())
        }

        coVerify(exactly = 0) {
            historyStorage.deleteEverything()
            iconsStorage.clear()
        }
    }

    @Ignore("Failing test; need more investigation.")
    @Test
    fun `delete everything and quit`() = runTestOnMain {
        // When
        every { settings.getDeleteDataOnQuit(TABS) } returns true
        every { settings.getDeleteDataOnQuit(HISTORY) } returns true
        every { settings.getDeleteDataOnQuit(COOKIES) } returns true
        every { settings.getDeleteDataOnQuit(CACHE) } returns true
        every { settings.getDeleteDataOnQuit(PERMISSIONS) } returns true
        every { settings.getDeleteDataOnQuit(DOWNLOADS) } returns true

        deleteAndQuit(activity, this, snackbar)

        advanceUntilIdle()

        coVerify(exactly = 1) {
            snackbar.show()

            // Delete tabs
            removeAllTabsUseCases.invoke(false)

            // Delete browsing data
            engine.clearData(Engine.BrowsingData.select(Engine.BrowsingData.DOM_STORAGES))
            historyStorage.deleteEverything()
            iconsStorage.clear()

            // Delete cookies
            engine.clearData(
                Engine.BrowsingData.select(
                    Engine.BrowsingData.COOKIES,
                    Engine.BrowsingData.AUTH_SESSIONS
                )
            )

            // Delete cached files
            engine.clearData(Engine.BrowsingData.select(Engine.BrowsingData.ALL_CACHES))

            // Delete permissions
            engine.clearData(Engine.BrowsingData.select(Engine.BrowsingData.ALL_SITE_SETTINGS))
            permissionStorage.deleteAllSitePermissions()

            // Delete downloads
            downloadsUseCases.invoke()

            // Finish activity
            activity.finishAndRemoveTask()
        }
    }
}
