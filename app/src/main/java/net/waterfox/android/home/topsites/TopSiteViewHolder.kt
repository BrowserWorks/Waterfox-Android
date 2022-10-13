/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.topsites

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.top.sites.TopSite
import net.waterfox.android.R
import net.waterfox.android.databinding.ComponentTopSitesBinding
import net.waterfox.android.home.sessioncontrol.TopSiteInteractor
import net.waterfox.android.utils.AccessibilityGridLayoutManager

class TopSiteViewHolder(
    view: View,
    viewLifecycleOwner: LifecycleOwner,
    interactor: TopSiteInteractor
) : RecyclerView.ViewHolder(view) {

    private val topSitesAdapter = TopSitesAdapter(viewLifecycleOwner, interactor)
    val binding = ComponentTopSitesBinding.bind(view)

    init {
        val gridLayoutManager =
            AccessibilityGridLayoutManager(view.context, SPAN_COUNT)

        binding.topSitesList.apply {
            adapter = topSitesAdapter
            layoutManager = gridLayoutManager
        }
    }

    fun bind(topSites: List<TopSite>) {
        topSitesAdapter.submitList(topSites)
    }

    companion object {
        const val LAYOUT_ID = R.layout.component_top_sites
        const val SPAN_COUNT = 4
    }
}
