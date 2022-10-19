/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.recentsyncedtabs.controller

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import mozilla.components.concept.sync.DeviceType
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.home.HomeFragmentDirections
import net.waterfox.android.home.recentsyncedtabs.RecentSyncedTab
import net.waterfox.android.tabstray.Page
import net.waterfox.android.tabstray.TabsTrayAccessPoint

@RunWith(AndroidJUnit4::class)
class DefaultRecentSyncedTabControllerTest {

    private val addTabUseCase: TabsUseCases.AddNewTabUseCase = mockk()
    private val navController: NavController = mockk()
    private val accessPoint = TabsTrayAccessPoint.HomeRecentSyncedTab

    private lateinit var controller: RecentSyncedTabController

    @Before
    fun setup() {
        controller = DefaultRecentSyncedTabController(
            addNewTabUseCase = addTabUseCase,
            navController = navController,
            accessPoint = accessPoint,
        )
    }

    @Test
    fun `WHEN synced tab clicked THEN tab add and navigate to browser`() {
        val url = "https://mozilla.org"
        val tab = RecentSyncedTab(
            deviceDisplayName = "display",
            deviceType = DeviceType.DESKTOP,
            title = "title",
            url = url,
            iconUrl = null
        )

        every { addTabUseCase.invoke(any()) } just runs
        every { navController.navigate(any<Int>()) } just runs

        controller.handleRecentSyncedTabClick(tab)

        verify { addTabUseCase.invoke(url) }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `WHEN synced tab show all clicked THEN navigate to synced tabs tray`() {
        every { navController.navigate(any<NavDirections>()) } just runs

        controller.handleSyncedTabShowAllClicked()

        verify {
            navController.navigate(
                HomeFragmentDirections.actionGlobalTabsTrayFragment(
                    page = Page.SyncedTabs,
                    accessPoint = accessPoint
                )
            )
        }
    }
}
