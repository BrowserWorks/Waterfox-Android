/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.tabcounter.TabCounter
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.browser.browsingmode.BrowsingModeManager
import net.waterfox.android.browser.browsingmode.DefaultBrowsingModeManager
import net.waterfox.android.ext.nav
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.utils.Settings

@RunWith(WaterfoxRobolectricTestRunner::class)
class TabCounterBuilderTest {

    private lateinit var navController: NavController
    private lateinit var browsingModeManager: BrowsingModeManager
    private lateinit var settings: Settings
    private lateinit var modeDidChange: (BrowsingMode) -> Unit
    private lateinit var tabCounterBuilder: TabCounterBuilder
    private lateinit var tabCounter: TabCounter

    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        modeDidChange = mockk(relaxed = true)

        tabCounter = TabCounter(testContext)

        browsingModeManager = DefaultBrowsingModeManager(
            _mode = BrowsingMode.Normal,
            settings = settings,
            modeDidChange = modeDidChange,
        )

        tabCounterBuilder = TabCounterBuilder(
            context = testContext,
            browsingModeManager = browsingModeManager,
            navController = navController,
            tabCounter = tabCounter,
        )
    }

    @Test
    fun `WHEN tab counter is clicked THEN navigate to tabs tray`() {
        tabCounterBuilder.build()

        tabCounter.performClick()

        verify {
            navController.nav(
                R.id.homeFragment,
                HomeFragmentDirections.actionGlobalTabsTrayFragment()
            )
        }
    }

    @Test
    fun `WHEN New tab menu item is tapped THEN set browsing mode to normal`() {
        tabCounterBuilder.onItemTapped(TabCounterMenu.Item.NewTab)

        assertEquals(BrowsingMode.Normal, browsingModeManager.mode)
    }

    @Test
    fun `WHEN New private tab menu item is tapped THEN set browsing mode to private`() {
        tabCounterBuilder.onItemTapped(TabCounterMenu.Item.NewPrivateTab)

        assertEquals(BrowsingMode.Private, browsingModeManager.mode)
    }
}
