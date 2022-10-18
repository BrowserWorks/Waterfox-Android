/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.nimbus.controller

import android.content.Context
import androidx.navigation.NavController
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.ui.NimbusBranchesAdapterDelegate
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.ext.getRootView
import net.waterfox.android.ext.settings
import net.waterfox.android.nimbus.NimbusBranchesAction
import net.waterfox.android.nimbus.NimbusBranchesStore
import org.mozilla.experiments.nimbus.Branch

/**
 * [NimbusBranchesFragment] controller. This implements [NimbusBranchesAdapterDelegate] to handle
 * interactions with a Nimbus branch.
 *
 * @param nimbusBranchesStore An instance of [NimbusBranchesStore] for dispatching
 * [NimbusBranchesAction]s.
 * @param experiments An instance of [NimbusApi] for interacting with the Nimbus experiments.
 * @param experimentId The string experiment-id or "slug" for a Nimbus experiment.
 */
class NimbusBranchesController(
    private val context: Context,
    private val navController: NavController,
    private val nimbusBranchesStore: NimbusBranchesStore,
    private val experiments: NimbusApi,
    private val experimentId: String
) : NimbusBranchesAdapterDelegate {

    override fun onBranchItemClicked(branch: Branch) {
        val experimentsEnabled = context.settings().isExperimentationEnabled

        updateOptInState(branch)

        if (!experimentsEnabled) {
            context.getRootView()?.let { v ->
                WaterfoxSnackbar.make(
                    view = v,
                    WaterfoxSnackbar.LENGTH_LONG,
                    isDisplayedWithBrowserToolbar = false
                )
                    .show()
            }
        }
    }

    private fun updateOptInState(branch: Branch) {
        nimbusBranchesStore.dispatch(
            if (experiments.getExperimentBranch(experimentId) != branch.slug) {
                experiments.optInWithBranch(experimentId, branch.slug)
                NimbusBranchesAction.UpdateSelectedBranch(branch.slug)
            } else {
                experiments.optOut(experimentId)
                NimbusBranchesAction.UpdateUnselectBranch
            }
        )
    }
}
