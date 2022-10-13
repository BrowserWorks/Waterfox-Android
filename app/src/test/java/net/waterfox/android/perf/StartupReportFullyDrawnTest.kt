/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.perf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.databinding.TopSiteItemBinding
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.home.topsites.TopSiteItemViewHolder
import net.waterfox.android.perf.StartupTimelineStateMachine.StartupDestination
import net.waterfox.android.perf.StartupTimelineStateMachine.StartupState

@RunWith(WaterfoxRobolectricTestRunner::class)
class StartupReportFullyDrawnTest {

    @MockK private lateinit var activity: HomeActivity
    private lateinit var holder: TopSiteItemViewHolder
    @MockK(relaxed = true) private lateinit var rootContainer: LinearLayout
    @MockK(relaxed = true) private lateinit var holderItemView: View
    @MockK(relaxed = true) private lateinit var viewTreeObserver: ViewTreeObserver
    private lateinit var fullyDrawn: StartupReportFullyDrawn

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val binding = TopSiteItemBinding.inflate(LayoutInflater.from(testContext), rootContainer, false)
        holderItemView = spyk(binding.root)
        every { activity.findViewById<LinearLayout>(R.id.rootContainer) } returns rootContainer
        every { holderItemView.context } returns activity
        holder = TopSiteItemViewHolder(holderItemView, mockk(), mockk())
        every { rootContainer.viewTreeObserver } returns viewTreeObserver
        every { holderItemView.viewTreeObserver } returns viewTreeObserver

        fullyDrawn = StartupReportFullyDrawn()
    }

    @Test
    fun testOnActivityCreateEndHome() {
        // Only APP_LINK destination
        fullyDrawn.onActivityCreateEndHome(StartupState.Cold(StartupDestination.UNKNOWN), activity)
        fullyDrawn.onActivityCreateEndHome(StartupState.Cold(StartupDestination.HOMESCREEN), activity)
        verify { activity wasNot Called }

        // Only run once
        fullyDrawn.onActivityCreateEndHome(StartupState.Cold(StartupDestination.APP_LINK), activity)
        verify(exactly = 1) { activity.findViewById<LinearLayout>(R.id.rootContainer) }

        fullyDrawn.onActivityCreateEndHome(StartupState.Cold(StartupDestination.APP_LINK), activity)
        verify(exactly = 1) { activity.findViewById<LinearLayout>(R.id.rootContainer) }

        every { activity.reportFullyDrawn() } just Runs
        triggerPreDraw()
        verify { activity.reportFullyDrawn() }
    }

    @Test
    fun testOnTopSitesItemBound() {
        fullyDrawn.onTopSitesItemBound(StartupState.Cold(StartupDestination.HOMESCREEN), holder)

        every { activity.reportFullyDrawn() } just Runs
        triggerPreDraw()
        verify { activity.reportFullyDrawn() }
    }

    private fun triggerPreDraw() {
        val listener = slot<ViewTreeObserver.OnPreDrawListener>()
        verify { viewTreeObserver.addOnPreDrawListener(capture(listener)) }
        listener.captured.onPreDraw()
    }
}
