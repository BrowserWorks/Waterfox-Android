/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.perf

import androidx.annotation.UiThread
import net.waterfox.android.HomeActivity
import net.waterfox.android.home.topsites.TopSiteItemViewHolder
import net.waterfox.android.perf.StartupTimeline.onApplicationInit
import net.waterfox.android.perf.StartupTimelineStateMachine.StartupActivity
import net.waterfox.android.perf.StartupTimelineStateMachine.StartupDestination
import net.waterfox.android.perf.StartupTimelineStateMachine.StartupState

/**
 * A collection of functionality to instrument, measure, and understand startup performance. The
 * responsibilities of this class are to update the internal [StartupState] based on the methods
 * called and to delegate calls to its dependencies, which handle other functionality related to
 * understanding startup.
 *
 * This class, and its dependencies, may need to be modified for any changes in startup.
 *
 * This class is not thread safe and should only be called from the main thread.
 *
 * [onApplicationInit] is called from multiple processes. To minimize overhead, the class
 * dependencies are lazily initialized.
 */
@UiThread
object StartupTimeline {

    private var state: StartupState = StartupState.Cold(StartupDestination.UNKNOWN)

    private val reportFullyDrawn by lazy { StartupReportFullyDrawn() }
    internal val frameworkStartMeasurement by lazy { ApplicationInitTimeContainer() }

    fun onApplicationInit() {
        // This gets called from multiple processes: don't do anything expensive. See call site for details.
        //
        // This method also gets called multiple times if there are multiple Application implementations.
        frameworkStartMeasurement.onApplicationInit()
    }

    fun onActivityCreateEndIntentReceiver() {
        advanceState(StartupActivity.INTENT_RECEIVER)
    }

    fun onActivityCreateEndHome(activity: HomeActivity) {
        advanceState(StartupActivity.HOME)
        reportFullyDrawn.onActivityCreateEndHome(state, activity)
    }

    fun onTopSitesItemBound(holder: TopSiteItemViewHolder) {
        // no advanceState associated with this method.
        reportFullyDrawn.onTopSitesItemBound(state, holder)
    }

    private fun advanceState(startingActivity: StartupActivity) {
        state = StartupTimelineStateMachine.getNextState(state, startingActivity)
    }
}
